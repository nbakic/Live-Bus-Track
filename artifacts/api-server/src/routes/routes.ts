import { Router, type IRouter } from "express";

const router: IRouter = Router();

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

interface OverpassResponse {
  elements: OverpassElement[];
}

const SPLIT_BBOX = "43.43,16.30,43.60,16.62";

// Cache route data for 10 minutes (routes don't change often)
const routeCache = new Map<string, { data: unknown; at: number }>();
const CACHE_TTL = 10 * 60 * 1000;

router.get("/routes/line/:lineName", async (req, res) => {
  const { lineName } = req.params;

  if (!lineName || lineName.length > 10) {
    return res.status(400).json({ error: "invalid_line", message: "Invalid line name" });
  }

  const cacheKey = lineName;
  const cached = routeCache.get(cacheKey);
  if (cached && Date.now() - cached.at < CACHE_TTL) {
    return res.json(cached.data);
  }

  try {
    // Query OSM Overpass API for bus route relations in Split area matching the line ref
    const overpassQuery = `[out:json][timeout:25];
relation[type=route][route=bus][ref="${lineName}"](${SPLIT_BBOX});
out geom;`;

    // Try multiple Overpass mirrors for better reliability
    const mirrors = [
      "https://overpass.kumi.systems/api/interpreter",
      "https://overpass-api.de/api/interpreter",
      "https://overpass.openstreetmap.ru/api/interpreter",
    ];

    let response: Response | null = null;
    let lastErr: unknown;

    for (const mirror of mirrors) {
      try {
        const res = await fetch(`${mirror}?data=${encodeURIComponent(overpassQuery)}`, {
          signal: AbortSignal.timeout(20000),
          headers: { "User-Agent": "SplitBusTracker/1.0" },
        });
        if (res.ok) {
          response = res;
          break;
        }
        req.log.warn({ status: res.status, mirror, lineName }, "Overpass mirror error");
      } catch (e) {
        lastErr = e;
        req.log.warn({ err: e, mirror, lineName }, "Overpass mirror failed, trying next");
      }
    }

    if (!response) {
      req.log.warn({ lastErr, lineName }, "All Overpass mirrors failed");
      return res.json({ routes: [], stops: [] });
    }

    if (!response.ok) {
      req.log.warn({ status: response.status, lineName }, "Overpass API error");
      return res.json({ routes: [], stops: [] });
    }

    const data = (await response.json()) as OverpassResponse;

    if (!data.elements || data.elements.length === 0) {
      const result = { routes: [], stops: [] };
      routeCache.set(cacheKey, { data: result, at: Date.now() });
      return res.json(result);
    }

    // Extract route geometry and stops from the best matching relation
    const relation = data.elements.find((e) => e.type === "relation" && e.members);
    if (!relation || !relation.members) {
      const result = { routes: [], stops: [] };
      routeCache.set(cacheKey, { data: result, at: Date.now() });
      return res.json(result);
    }

    // Build list of route coordinate segments (ways)
    const segments: Array<Array<[number, number]>> = [];
    const stops: Array<{ lat: number; lon: number; name?: string; role: string }> = [];

    for (const member of relation.members) {
      if (member.type === "way" && member.geometry && member.geometry.length > 0) {
        const coords: Array<[number, number]> = member.geometry.map((p) => [p.lat, p.lon]);
        segments.push(coords);
      } else if (member.type === "node" && member.lat != null && member.lon != null) {
        stops.push({
          lat: member.lat,
          lon: member.lon,
          role: member.role || "stop",
        });
      }
    }

    const result = { routes: segments, stops };
    routeCache.set(cacheKey, { data: result, at: Date.now() });
    return res.json(result);
  } catch (err) {
    req.log.warn({ err, lineName }, "Failed to fetch route from Overpass");
    return res.json({ routes: [], stops: [] });
  }
});

export default router;
