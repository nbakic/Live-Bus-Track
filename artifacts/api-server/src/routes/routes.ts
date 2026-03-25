import { Router, type IRouter } from "express";

const router: IRouter = Router();

const PROMET_API_BASE = "https://api.promet-split.hr/Fleet";
const AUTH_KEY = "IxbMAfY6J5x1rSyfGmLPcMfCcyamb7xEfIuUpb8KNeE=";
const SPLIT_BBOX = "43.43,16.30,43.60,16.62";

interface OverpassElement {
  type: string;
  id: number;
  tags?: Record<string, string>;
  members?: Array<{
    type: string;
    ref: number;
    role: string;
    geometry?: Array<{ lat: number; lon: number }>;
    lat?: number;
    lon?: number;
  }>;
}

interface PrometVehicle {
  id: number;
  latitude: number;
  longitude: number;
  name: string;
  vehicleStatus: number;
}

export interface RouteResult {
  routes: Array<Array<[number, number]>>;
  stops: Array<{ lat: number; lon: number; name?: string; role: string }>;
  source: "osm" | "osrm" | "none";
}

// Cache route data for 8 minutes
const routeCache = new Map<string, { data: RouteResult; at: number }>();
const CACHE_TTL = 8 * 60 * 1000;

// --- OSM Overpass fetch ---
async function fetchFromOSM(lineName: string, log: typeof console): Promise<RouteResult | null> {
  const overpassQuery = `[out:json][timeout:20];
relation[type=route][route=bus][ref="${lineName}"](${SPLIT_BBOX});
out geom;`;

  const mirrors = [
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass-api.de/api/interpreter",
  ];

  for (const mirror of mirrors) {
    try {
      const res = await fetch(`${mirror}?data=${encodeURIComponent(overpassQuery)}`, {
        signal: AbortSignal.timeout(15000),
        headers: { "User-Agent": "SplitBusTracker/1.0" },
      });
      if (!res.ok) continue;

      const data = (await res.json()) as { elements: OverpassElement[] };
      if (!data.elements || data.elements.length === 0) return null;

      const relation = data.elements.find((e) => e.type === "relation" && e.members);
      if (!relation?.members) return null;

      const segments: Array<Array<[number, number]>> = [];
      const stops: Array<{ lat: number; lon: number; role: string }> = [];

      for (const member of relation.members) {
        if (member.type === "way" && member.geometry && member.geometry.length > 0) {
          segments.push(member.geometry.map((p) => [p.lat, p.lon]));
        } else if (member.type === "node" && member.lat != null && member.lon != null) {
          stops.push({ lat: member.lat, lon: member.lon, role: member.role || "stop" });
        }
      }

      if (segments.length === 0) return null;
      return { routes: segments, stops, source: "osm" };
    } catch {
      // try next mirror
    }
  }
  return null;
}

// --- OSRM routing fallback ---
// Sorts vehicle positions into a plausible route order using a nearest-neighbour heuristic,
// then calls OSRM to snap them to actual roads.
async function fetchFromOSRM(lineName: string, log: typeof console): Promise<RouteResult | null> {
  try {
    // Fetch live vehicles from upstream
    const ts = Date.now();
    const res = await fetch(`${PROMET_API_BASE}/api/v1/live/vehicles?t=${ts}`, {
      headers: { "x-auth-key": AUTH_KEY, "Accept": "application/json" },
      signal: AbortSignal.timeout(8000),
    });
    if (!res.ok) return null;

    const all = (await res.json()) as PrometVehicle[];
    const lineVehicles = all.filter(
      (v) => String(v.name).trim() === lineName && v.latitude && v.longitude
    );

    if (lineVehicles.length < 2) return null;

    // Deduplicate by approximate position (snap to 3 decimal places)
    const seen = new Set<string>();
    const unique = lineVehicles.filter((v) => {
      const key = `${v.latitude.toFixed(3)},${v.longitude.toFixed(3)}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });

    if (unique.length < 2) return null;

    // Nearest-neighbour sort starting from westernmost point
    const sorted = [...unique].sort((a, b) => a.longitude - b.longitude);
    const ordered: PrometVehicle[] = [sorted[0]];
    const remaining = sorted.slice(1);
    while (remaining.length > 0) {
      const last = ordered[ordered.length - 1];
      let nearestIdx = 0;
      let minDist = Infinity;
      for (let i = 0; i < remaining.length; i++) {
        const d = Math.hypot(remaining[i].latitude - last.latitude, remaining[i].longitude - last.longitude);
        if (d < minDist) { minDist = d; nearestIdx = i; }
      }
      ordered.push(remaining[nearestIdx]);
      remaining.splice(nearestIdx, 1);
    }

    // OSRM route (max 10 waypoints to keep request small)
    const waypoints = ordered.slice(0, 10);
    const coords = waypoints.map((v) => `${v.longitude},${v.latitude}`).join(";");
    const osrmUrl = `http://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`;

    const osrmRes = await fetch(osrmUrl, { signal: AbortSignal.timeout(10000) });
    if (!osrmRes.ok) return null;

    const osrmData = (await osrmRes.json()) as {
      code: string;
      routes?: Array<{ geometry: { coordinates: Array<[number, number]> } }>;
    };

    if (osrmData.code !== "Ok" || !osrmData.routes?.[0]) return null;

    // OSRM returns [lng, lat] — convert to [lat, lng] for Leaflet
    const coords2 = osrmData.routes[0].geometry.coordinates.map(
      ([lng, lat]) => [lat, lng] as [number, number]
    );

    // Use vehicle positions as stop markers
    const stops = ordered.map((v) => ({ lat: v.latitude, lon: v.longitude, role: "stop" }));

    return { routes: [coords2], stops, source: "osrm" };
  } catch (err) {
    log.warn?.({ err, lineName }, "OSRM fallback failed");
    return null;
  }
}

router.get("/routes/line/:lineName", async (req, res) => {
  const { lineName } = req.params;
  if (!lineName || lineName.length > 10) {
    return res.status(400).json({ error: "invalid_line", message: "Invalid line name" });
  }

  const cached = routeCache.get(lineName);
  if (cached && Date.now() - cached.at < CACHE_TTL) {
    return res.json(cached.data);
  }

  try {
    // 1. Try OSM Overpass (best quality)
    const osmResult = await fetchFromOSM(lineName, req.log as typeof console);
    if (osmResult) {
      routeCache.set(lineName, { data: osmResult, at: Date.now() });
      return res.json(osmResult);
    }

    // 2. Fallback: OSRM routing between current vehicle positions
    const osrmResult = await fetchFromOSRM(lineName, req.log as typeof console);
    if (osrmResult) {
      // Cache OSRM results for shorter time since vehicle positions change
      routeCache.set(lineName, { data: osrmResult, at: Date.now() - CACHE_TTL + 2 * 60 * 1000 });
      return res.json(osrmResult);
    }

    const empty: RouteResult = { routes: [], stops: [], source: "none" };
    return res.json(empty);
  } catch (err) {
    req.log.warn({ err, lineName }, "Route fetch failed entirely");
    return res.json({ routes: [], stops: [], source: "none" });
  }
});

export default router;
