import { Router, type IRouter } from "express";

const router: IRouter = Router();

const PROMET_API_BASE = "https://api.promet-split.hr/Fleet";
const AUTH_KEY = "IxbMAfY6J5x1rSyfGmLPcMfCcyamb7xEfIuUpb8KNeE=";

interface PrometVehicle {
  id: number;
  timestamp: string;
  garageNumber: string;
  registrationNumber: string;
  name: string;
  latitude: number;
  longitude: number;
  vehicleStatus: number;
}

function generateDemoVehicles(): PrometVehicle[] {
  const lines = [
    "1", "2", "5", "7", "9", "10", "12", "15", "20", "21",
    "22", "25", "26", "27", "28", "37", "60", "N",
  ];
  const statuses = [1, 1, 1, 2, 3, 6];
  const centerLat = 43.513;
  const centerLng = 16.45;
  const vehicles: PrometVehicle[] = [];
  let id = 100000;
  for (const line of lines) {
    const count = Math.floor(Math.random() * 4) + 2;
    for (let i = 0; i < count; i++) {
      vehicles.push({
        id: id++,
        timestamp: new Date().toISOString(),
        garageNumber: String(id % 200).padStart(3, "0"),
        registrationNumber: `ST ${Math.floor(Math.random() * 9000) + 1000} P`,
        name: line,
        latitude: centerLat + (Math.random() - 0.5) * 0.12,
        longitude: centerLng + (Math.random() - 0.5) * 0.18,
        vehicleStatus: statuses[Math.floor(Math.random() * statuses.length)],
      });
    }
  }
  return vehicles;
}

router.get("/vehicles/live", async (req, res) => {
  const fetchedAt = new Date().toISOString();
  const ts = Date.now();

  try {
    const url = `${PROMET_API_BASE}/api/v1/live/vehicles?t=${ts}`;
    const response = await fetch(url, {
      headers: {
        "x-auth-key": AUTH_KEY,
        "Accept": "application/json",
        "User-Agent": "SplitBusTracker/1.0",
      },
      signal: AbortSignal.timeout(8000),
    });

    if (!response.ok) {
      req.log.warn({ status: response.status }, "Upstream API returned error, using demo data");
      return res.json({
        vehicles: generateDemoVehicles(),
        fetchedAt,
        source: "demo",
      });
    }

    const data = (await response.json()) as PrometVehicle[];

    if (!Array.isArray(data)) {
      req.log.warn({ data }, "Unexpected response shape from upstream, using demo data");
      return res.json({
        vehicles: generateDemoVehicles(),
        fetchedAt,
        source: "demo",
      });
    }

    const validVehicles = data.filter(
      (v) =>
        v &&
        typeof v.id === "number" &&
        typeof v.latitude === "number" &&
        typeof v.longitude === "number" &&
        v.name != null &&
        v.name !== "null" &&
        String(v.name).trim() !== ""
    );

    res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    res.setHeader("Pragma", "no-cache");
    return res.json({
      vehicles: validVehicles,
      fetchedAt,
      source: "live",
    });
  } catch (err) {
    req.log.warn({ err }, "Failed to fetch from upstream API, using demo data");
    res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    return res.json({
      vehicles: generateDemoVehicles(),
      fetchedAt,
      source: "demo",
    });
  }
});

export default router;
