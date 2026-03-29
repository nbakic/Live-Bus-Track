import { cn } from "@/lib/utils";
import type { Vehicle } from "@workspace/api-client-react";

interface FilterBarProps {
  lines: string[];
  selectedLine: string | null;
  onSelectLine: (line: string | null) => void;
  vehicles: Vehicle[];
}

export function FilterBar({ lines, selectedLine, onSelectLine, vehicles }: FilterBarProps) {
  
  // Count vehicles per line
  const counts = vehicles.reduce((acc, v) => {
    acc[v.name] = (acc[v.name] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="absolute bottom-0 left-0 right-0 z-[1000] pointer-events-none px-4 pb-[calc(env(safe-area-inset-bottom,0px)+4rem)]">
      <div className="max-w-4xl mx-auto">
        <div className="bg-background/80 backdrop-blur-xl border border-white/10 shadow-2xl rounded-2xl p-2 pointer-events-auto overflow-hidden flex">
          <div className="flex overflow-x-auto gap-2 pb-1 scrollbar-hide no-scrollbar snap-x" style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
            
            {/* SVE (All) Chip */}
            <button
              onClick={() => onSelectLine(null)}
              className={cn(
                "snap-start shrink-0 px-4 py-2 rounded-xl text-sm font-bold transition-all duration-200 flex items-center gap-2",
                selectedLine === null 
                  ? "bg-white text-black shadow-md shadow-white/20" 
                  : "bg-white/5 text-white hover:bg-white/10 border border-white/5"
              )}
            >
              SVE
              <span className={cn(
                "text-[10px] px-1.5 py-0.5 rounded-full font-mono",
                selectedLine === null ? "bg-black/10" : "bg-black/30"
              )}>
                {vehicles.length}
              </span>
            </button>

            <div className="w-px shrink-0 bg-white/10 my-2 mx-1" />

            {/* Individual Line Chips */}
            {lines.map((line) => (
              <button
                key={line}
                onClick={() => onSelectLine(selectedLine === line ? null : line)}
                className={cn(
                  "snap-start shrink-0 px-4 py-2 rounded-xl text-sm font-bold transition-all duration-200 flex items-center gap-2",
                  selectedLine === line 
                    ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20" 
                    : "bg-white/5 text-white hover:bg-white/10 border border-white/5"
                )}
              >
                {line}
                {counts[line] > 0 && (
                  <span className={cn(
                    "text-[10px] px-1.5 py-0.5 rounded-full font-mono",
                    selectedLine === line ? "bg-black/20" : "bg-black/30 text-white/70"
                  )}>
                    {counts[line]}
                  </span>
                )}
              </button>
            ))}
            
            {/* Pad end for scrolling comfort */}
            <div className="w-4 shrink-0" />
          </div>
        </div>
      </div>
    </div>
  );
}
