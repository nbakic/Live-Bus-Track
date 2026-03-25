import { Router, type IRouter } from "express";
import staticRoutesRaw from "../data/routes.json";

const router: IRouter = Router();

const PROMET_API_BASE = "https://api.promet-split.hr/Fleet";
const AUTH_KEY = "IxbMAfY6J5x1rSyfGmLPcMfCcyamb7xEfIuUpb8KNeE=";
const SPLIT_BBOX = "43.43,16.30,43.60,16.62";

// Type the static bundle
interface StaticRouteEntry {
  routes: Array<Array<[number, number]>>;
  stops: Array<{ lat: number; lon: number; role: string }>;
  source: "osm";
}
const staticRoutes = staticRoutesRaw.data as Record<string, StaticRouteEntry>;

export interface RouteResult {
  routes: Array<Array<[number, number]>>;
  stops: Array<{ lat: number; lon: number; name?: string; role: string }>;
  source: "osm" | "osrm" | "none";
}

// Small cache only for OSRM results (static OSM data never changes)
const osrmCache = new Map<string, { data: RouteResult; at: number }>();
const OSRM_TTL = 3 * 60 * 1000;

interface PrometVehicle {
  id: number;
  latitude: number;
  longitude: number;
  name: string;
  vehicleStatus: number;
}

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
      const data = (await res.json()) as {
        elements: Array<{
          type: string;
          members?: Array<{
            type: string; ref: number; role: string;
            geometry?: Array<{ lat: number; lon: number }>;
            lat?: number; lon?: number;
          }>;
        }>;
      };
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
    } catch { /* try next */ }
  }
  return null;
}

async function fetchOSRMRoute(lineName: string): Promise<RouteResult | null> {
  const cached = osrmCache.get(lineName);
  if (cached && Date.now() - cached.at < OSRM_TTL) return cached.data;

  try {
    const res = await fetch(`${PROMET_API_BASE}/api/v1/live/vehicles?t=${Date.now()}`, {
      headers: { "x-auth-key": AUTH_KEY, Accept: "application/json" },
      signal: AbortSignal.timeout(6000),
    });
    if (!res.ok) return null;
    const all = (await res.json()) as PrometVehicle[];
    const lineVehicles = all.filter(
      (v) => String(v.name).trim() === lineName && v.latitude && v.longitude
    );
    if (lineVehicles.length < 2) return null;

    const seen = new Set<string>();
    const unique = lineVehicles.filter((v) => {
      const k = `${v.latitude.toFixed(3)},${v.longitude.toFixed(3)}`;
      if (seen.has(k)) return false; seen.add(k); return true;
    });
    if (unique.length < 2) return null;

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
    const osrmRes = await fetch(
      `http://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`,
      { signal: AbortSignal.timeout(7000) }
    );
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
    const result: RouteResult = { routes: [routeCoords], stops, source: "osrm" };
    osrmCache.set(lineName, { data: result, at: Date.now() });
    return result;
  } catch {
    return null;
  }
}

router.get("/routes/line/:lineName", async (req, res) => {
  const { lineName } = req.params;
  if (!lineName || lineName.length > 10) {
    return res.status(400).json({ error: "invalid_line" });
  }

  // 1. Fastest: static pre-bundled OSM data (no network call, instant)
  const staticEntry = staticRoutes[lineName];
  if (staticEntry?.routes?.length) {
    return res.json(staticEntry);
  }

  // 2. Line not in static bundle — try live Overpass and OSRM in parallel
  try {
    const [osmResult, osrmResult] = await Promise.all([
      fetchFromOSM(lineName).catch(() => null),
      fetchOSRMRoute(lineName).catch(() => null),
    ]);
    if (osmResult) return res.json(osmResult);
    if (osrmResult) return res.json(osrmResult);
  } catch (err) {
    req.log.warn({ err, lineName }, "Route fetch failed");
  }

  return res.json({ routes: [], stops: [], source: "none" } as RouteResult);
});

export default router;
