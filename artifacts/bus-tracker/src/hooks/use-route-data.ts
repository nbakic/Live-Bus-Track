import { useQuery } from "@tanstack/react-query";

export interface RouteData {
  routes: Array<Array<[number, number]>>;
  stops: Array<{ lat: number; lon: number; name?: string; role: string }>;
  source: "osm" | "osrm" | "none";
}

async function fetchRouteData(lineName: string): Promise<RouteData> {
  const baseUrl = import.meta.env.BASE_URL.replace(/\/$/, "");
  const res = await fetch(`${baseUrl}/api/routes/line/${encodeURIComponent(lineName)}`);
  if (!res.ok) throw new Error("Route fetch failed");
  return res.json();
}

export function useRouteData(lineName: string | null) {
  return useQuery<RouteData>({
    queryKey: ["route", lineName],
    queryFn: () => fetchRouteData(lineName!),
    enabled: !!lineName,
    staleTime: 8 * 60 * 1000,
    retry: 1,
  });
}
