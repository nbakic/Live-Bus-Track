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

// Cache: 8 min for OSM, 2 min for OSRM (vehicles move)
const routeCache = new Map<string, { data: RouteResult; at: number; ttl: number }>();

// --- OSM Overpass ---
async function fetchFromOSM(lineName: string): Promise<RouteResult | null> {
  const query = `[out:json][timeout:15];
relation[type=route][route=bus][ref="${lineName}"](${SPLIT_BBOX});
out geom;`;

  const mirrors = [
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass-api.de/api/interpreter",
  ];

  for (const mirror of mirrors) {
    try {
      const res = await fetch(`${mirror}?data=${encodeURIComponent(query)}`, {
        signal: AbortSignal.timeout(10000),
        headers: { "User-Agent": "SplitBusTracker/1.0" },
      });
      if (!res.ok) continue;
      const data = (await res.json()) as { elements: OverpassElement[] };
      if (!data.elements?.length) return null;

      const relation = data.elements.find((e) => e.type === "relation" && e.members);
      if (!relation?.members) return null;

      const segments: Array<Array<[number, number]>> = [];
      const stops: Array<{ lat: number; lon: number; role: string }> = [];

      for (const m of relation.members) {
        if (m.type === "way" && m.geometry?.length) {
          segments.push(m.geometry.map((p) => [p.lat, p.lon]));
        } else if (m.type === "node" && m.lat != null && m.lon != null) {
          stops.push({ lat: m.lat, lon: m.lon, role: m.role || "stop" });
        }
      }

      if (!segments.length) return null;
      return { routes: segments, stops, source: "osm" };
    } catch {
      // try next mirror
    }
  }
  return null;
}

// --- Fetch vehicle positions for line (standalone, for OSRM) ---
async function fetchVehiclesForLine(lineName: string): Promise<PrometVehicle[]> {
  const res = await fetch(`${PROMET_API_BASE}/api/v1/live/vehicles?t=${Date.now()}`, {
    headers: { "x-auth-key": AUTH_KEY, Accept: "application/json" },
    signal: AbortSignal.timeout(6000),
  });
  if (!res.ok) return [];
  const all = (await res.json()) as PrometVehicle[];
  return all.filter((v) => String(v.name).trim() === lineName && v.latitude && v.longitude);
}

// --- OSRM routing fallback ---
async function fetchFromOSRM(lineName: string): Promise<RouteResult | null> {
  try {
    const lineVehicles = await fetchVehiclesForLine(lineName);
    if (lineVehicles.length < 2) return null;

    // Deduplicate by ~3 decimal places
    const seen = new Set<string>();
    const unique = lineVehicles.filter((v) => {
      const k = `${v.latitude.toFixed(3)},${v.longitude.toFixed(3)}`;
      if (seen.has(k)) return false;
      seen.add(k);
      return true;
    });
    if (unique.length < 2) return null;

    // Nearest-neighbour sort from westernmost
    const sorted = [...unique].sort((a, b) => a.longitude - b.longitude);
    const ordered: PrometVehicle[] = [sorted[0]];
    const remaining = sorted.slice(1);
    while (remaining.length) {
      const last = ordered[ordered.length - 1];
      let ni = 0, min = Infinity;
      for (let i = 0; i < remaining.length; i++) {
        const d = Math.hypot(remaining[i].lat - last.latitude, remaining[i].lon - last.longitude);
        if (d < min) { min = d; ni = i; }
      }
      ordered.push(remaining[ni]);
      remaining.splice(ni, 1);
    }

    const waypoints = ordered.slice(0, 10);
    const coords = waypoints.map((v) => `${v.longitude},${v.latitude}`).join(";");
    const url = `http://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`;

    const osrmRes = await fetch(url, { signal: AbortSignal.timeout(7000) });
    if (!osrmRes.ok) return null;

    const osrmData = (await osrmRes.json()) as {
      code: string;
      routes?: Array<{ geometry: { coordinates: Array<[number, number]> } }>;
    };

    if (osrmData.code !== "Ok" || !osrmData.routes?.[0]) return null;

    const routeCoords = osrmData.routes[0].geometry.coordinates.map(
      ([lng, lat]) => [lat, lng] as [number, number]
    );
    const stops = ordered.map((v) => ({ lat: v.latitude, lon: v.longitude, role: "stop" }));
    return { routes: [routeCoords], stops, source: "osrm" };
  } catch {
    return null;
  }
}

router.get("/routes/line/:lineName", async (req, res) => {
  const { lineName } = req.params;
  if (!lineName || lineName.length > 10) {
    return res.status(400).json({ error: "invalid_line" });
  }

  const cached = routeCache.get(lineName);
  if (cached && Date.now() - cached.at < cached.ttl) {
    return res.json(cached.data);
  }

  try {
    // Run OSM and vehicle-fetch in parallel; OSRM routing follows vehicle-fetch if OSM fails
    const [osmResult, lineVehicles] = await Promise.all([
      fetchFromOSM(lineName).catch(() => null),
      fetchVehiclesForLine(lineName).catch(() => []),
    ]);

    if (osmResult) {
      routeCache.set(lineName, { data: osmResult, at: Date.now(), ttl: 8 * 60 * 1000 });
      return res.json(osmResult);
    }

    // OSM failed — try OSRM using already-fetched vehicles
    if (lineVehicles.length >= 2) {
      // Reuse the vehicles already fetched; build a mini-OSRM call
      const seen = new Set<string>();
      const unique = lineVehicles.filter((v) => {
        const k = `${v.latitude.toFixed(3)},${v.longitude.toFixed(3)}`;
        if (seen.has(k)) return false; seen.add(k); return true;
      });

      if (unique.length >= 2) {
        const sorted = [...unique].sort((a, b) => a.longitude - b.longitude);
        const ordered: PrometVehicle[] = [sorted[0]];
        const remaining = sorted.slice(1);
        while (remaining.length) {
          const last = ordered[ordered.length - 1];
          let ni = 0, min = Infinity;
          for (let i = 0; i < remaining.length; i++) {
            const d = Math.hypot(remaining[i].latitude - last.latitude, remaining[i].longitude - last.longitude);
            if (d < min) { min = d; ni = i; }
          }
          ordered.push(remaining[ni]);
          remaining.splice(ni, 1);
        }

        const waypoints = ordered.slice(0, 10);
        const coords = waypoints.map((v) => `${v.longitude},${v.latitude}`).join(";");
        try {
          const osrmRes = await fetch(
            `http://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`,
            { signal: AbortSignal.timeout(7000) }
          );
          if (osrmRes.ok) {
            const osrmData = (await osrmRes.json()) as {
              code: string;
              routes?: Array<{ geometry: { coordinates: Array<[number, number]> } }>;
            };
            if (osrmData.code === "Ok" && osrmData.routes?.[0]) {
              const routeCoords = osrmData.routes[0].geometry.coordinates.map(
                ([lng, lat]) => [lat, lng] as [number, number]
              );
              const stops = ordered.map((v) => ({ lat: v.latitude, lon: v.longitude, role: "stop" }));
              const result: RouteResult = { routes: [routeCoords], stops, source: "osrm" };
              routeCache.set(lineName, { data: result, at: Date.now(), ttl: 2 * 60 * 1000 });
              return res.json(result);
            }
          }
        } catch { /* fall through */ }
      }
    }

    const empty: RouteResult = { routes: [], stops: [], source: "none" };
    routeCache.set(lineName, { data: empty, at: Date.now(), ttl: 60 * 1000 });
    return res.json(empty);
  } catch (err) {
    req.log.warn({ err, lineName }, "Route fetch failed");
    return res.json({ routes: [], stops: [], source: "none" });
  }
});

export default router;
