import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";

export interface RouteData {
  routes: Array<Array<[number, number]>>;
  stops: Array<{ lat: number; lon: number; name?: string; role: string }>;
  source: "osm" | "osrm" | "none";
}

interface AllRoutesBundle {
  data: Record<string, RouteData>;
}

const ALL_ROUTES_KEY = ["all-routes"] as const;

async function fetchAllRoutes(): Promise<AllRoutesBundle> {
  const baseUrl = import.meta.env.BASE_URL.replace(/\/$/, "");
  const res = await fetch(`${baseUrl}/routes.json`);
  if (!res.ok) throw new Error("Failed to load routes bundle");
  return res.json();
}

/**
 * Pre-loads the full routes bundle once at app startup.
 * Call this in a top-level component (e.g. Home).
 */
export function usePrefetchRoutes() {
  const queryClient = useQueryClient();

  useEffect(() => {
    queryClient.prefetchQuery({
      queryKey: ALL_ROUTES_KEY,
      queryFn: fetchAllRoutes,
      staleTime: Infinity,
    });
  }, [queryClient]);
}

/**
 * Returns route data for a specific line.
 * Reads from the pre-loaded bundle (instant) with API fallback.
 */
export function useRouteData(lineName: string | null) {
  const queryClient = useQueryClient();

  return useQuery<RouteData>({
    queryKey: ["route", lineName],
    queryFn: async () => {
      // Try the pre-loaded bundle first
      const bundle = queryClient.getQueryData<AllRoutesBundle>(ALL_ROUTES_KEY);
      if (bundle?.data[lineName!]) {
        return bundle.data[lineName!];
      }

      // Fallback: fetch from API (for lines not in static bundle)
      const baseUrl = import.meta.env.BASE_URL.replace(/\/$/, "");
      const res = await fetch(
        `${baseUrl}/api/routes/line/${encodeURIComponent(lineName!)}`,
      );
      if (!res.ok) throw new Error("Route fetch failed");
      return res.json();
    },
    enabled: !!lineName,
    staleTime: Infinity,
    retry: 1,
  });
}
