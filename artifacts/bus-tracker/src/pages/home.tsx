import { useState } from "react";
import { useBusData } from "@/hooks/use-bus-data";
import { BusMap } from "@/components/map/BusMap";
import { StatusBar } from "@/components/layout/StatusBar";
import { FilterBar } from "@/components/layout/FilterBar";
import { Loader2, AlertCircle, RefreshCw } from "lucide-react";

export default function Home() {
  const { data, isLoading, isError, isStale, dataUpdatedAt, uniqueLines, refetch } = useBusData();
  const [selectedLine, setSelectedLine] = useState<string | null>(null);

  const allVehicles = data?.vehicles || [];
  
  // Filter vehicles by selected line
  const visibleVehicles = selectedLine 
    ? allVehicles.filter(v => v.name === selectedLine)
    : allVehicles;

  return (
    <main className="relative w-full h-screen overflow-hidden bg-background">
      
      {/* Absolute Overlays Layer - Higher Z-index */}
      <div className="absolute inset-0 pointer-events-none z-[100]">
        <StatusBar 
          isLoading={isLoading}
          isError={isError}
          isStale={isStale}
          lastUpdated={dataUpdatedAt}
          vehicleCount={visibleVehicles.length}
          source={data?.source}
        />

        {data && (
          <FilterBar 
            lines={uniqueLines}
            selectedLine={selectedLine}
            onSelectLine={setSelectedLine}
            vehicles={allVehicles}
          />
        )}
      </div>

      {/* Initial Loading State */}
      {isLoading && !data && (
        <div className="absolute inset-0 z-[500] flex flex-col items-center justify-center bg-background/80 backdrop-blur-sm animate-fade-in">
          <Loader2 className="w-12 h-12 text-primary animate-spin mb-4" />
          <h2 className="text-xl font-bold font-display text-foreground">Učitavanje autobusa...</h2>
          <p className="text-muted-foreground mt-2">Povezivanje s Promet Split sustavom</p>
        </div>
      )}

      {/* Fatal Error State */}
      {isError && !data && (
        <div className="absolute inset-0 z-[500] flex flex-col items-center justify-center bg-background/90 backdrop-blur-md animate-fade-in p-6 text-center">
          <div className="w-20 h-20 bg-destructive/20 rounded-full flex items-center justify-center mb-6">
            <AlertCircle className="w-10 h-10 text-destructive" />
          </div>
          <h2 className="text-2xl font-bold font-display text-foreground mb-2">Greška pri dohvaćanju podataka</h2>
          <p className="text-muted-foreground mb-8 max-w-md">
            Nije moguće uspostaviti vezu s API poslužiteljem. Provjerite internetsku vezu ili pokušajte ponovno kasnije.
          </p>
          <button 
            onClick={() => refetch()}
            className="flex items-center gap-2 px-6 py-3 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl shadow-lg transition-all active:scale-95"
          >
            <RefreshCw className="w-5 h-5" />
            Pokušaj ponovno
          </button>
        </div>
      )}

      {/* The Map */}
      <BusMap vehicles={visibleVehicles} />
      
    </main>
  );
}
