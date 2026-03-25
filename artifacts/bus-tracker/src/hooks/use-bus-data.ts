import { useMemo } from "react";
import { useGetLiveVehicles } from "@workspace/api-client-react";

export function useBusData() {
  const query = useGetLiveVehicles({
    query: {
      refetchInterval: 5000, // Poll every 5 seconds
      staleTime: 4000,
    }
  });

  const isStale = useMemo(() => {
    if (!query.dataUpdatedAt) return false;
    return Date.now() - query.dataUpdatedAt > 30000; // Warning if >30s old
  }, [query.dataUpdatedAt, Date.now()]); // Note: Date.now() won't trigger re-render alone, but dataUpdatedAt will.

  const uniqueLines = useMemo(() => {
    if (!query.data?.vehicles) return [];
    
    const lines = Array.from(new Set(query.data.vehicles.map(v => v.name)));
    
    // Sort numeric lines first (1, 2, 3...), then alphanumeric (10A, 15, 60...)
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
    isStale,
    uniqueLines,
  };
}
