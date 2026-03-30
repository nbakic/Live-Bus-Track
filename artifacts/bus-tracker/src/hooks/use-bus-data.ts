import { useMemo, useRef } from "react";
import { useGetLiveVehicles } from "@workspace/api-client-react";
import type { Vehicle } from "@workspace/api-client-react";

export type VehicleWithBearing = Vehicle & { bearing: number | null };

/** Compute bearing (degrees 0-360, 0=north) between two lat/lon points */
function computeBearing(
  lat1: number, lon1: number,
  lat2: number, lon2: number,
): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const toDeg = (r: number) => (r * 180) / Math.PI;
  const dLon = toRad(lon2 - lon1);
  const y = Math.sin(dLon) * Math.cos(toRad(lat2));
  const x =
    Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
    Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(dLon);
  return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

/** Approximate distance in meters between two lat/lon points */
function distanceMeters(
  lat1: number, lon1: number,
  lat2: number, lon2: number,
): number {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

interface PrevPos {
  lat: number;
  lon: number;
  bearing: number | null;
}

export function useBusData() {
  const query = useGetLiveVehicles({
    query: {
      queryKey: ["live-vehicles"] as const,
      refetchInterval: 5000,
      staleTime: 4000,
    }
  });

  const prevRef = useRef<Map<number, PrevPos>>(new Map());

  const vehiclesWithBearing = useMemo((): VehicleWithBearing[] => {
    if (!query.data?.vehicles) return [];
    const prev = prevRef.current;
    const next = new Map<number, PrevPos>();

    const result = query.data.vehicles.map((v) => {
      const old = prev.get(v.id);
      let bearing: number | null = old?.bearing ?? null;

      if (old && distanceMeters(old.lat, old.lon, v.latitude, v.longitude) > 8) {
        bearing = computeBearing(old.lat, old.lon, v.latitude, v.longitude);
      }

      next.set(v.id, { lat: v.latitude, lon: v.longitude, bearing });
      return { ...v, bearing };
    });

    prevRef.current = next;
    return result;
  }, [query.data?.vehicles]);

  const isStale = useMemo(() => {
    if (!query.dataUpdatedAt) return false;
    return Date.now() - query.dataUpdatedAt > 30000;
  }, [query.dataUpdatedAt, Date.now()]);

  const uniqueLines = useMemo(() => {
    if (!query.data?.vehicles) return [];

    const lines = Array.from(new Set(query.data.vehicles.map(v => v.name)));

    return lines.sort((a, b) => {
      const numA = parseInt(a);
      const numB = parseInt(b);

      const isNumA = !isNaN(numA);
      const isNumB = !isNaN(numB);

      if (isNumA && isNumB) {
        if (numA === numB) {
          return a.localeCompare(b);
        }
        return numA - numB;
      }

      if (isNumA) return -1;
      if (isNumB) return 1;

      return a.localeCompare(b);
    });
  }, [query.data?.vehicles]);

  return {
    ...query,
    vehiclesWithBearing,
    isStale,
    uniqueLines,
  };
}
