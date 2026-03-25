import { Wifi, WifiOff, AlertTriangle, Clock } from "lucide-react";
import { format } from "date-fns";
import { cn } from "@/lib/utils";

interface StatusBarProps {
  isLoading: boolean;
  isError: boolean;
  isStale: boolean;
  lastUpdated: number;
  vehicleCount: number;
  source?: string;
}

export function StatusBar({ isLoading, isError, isStale, lastUpdated, vehicleCount, source }: StatusBarProps) {
  const formattedTime = lastUpdated ? format(lastUpdated, "HH:mm:ss") : "--:--:--";
  
  let statusColor = "bg-green-500";
  let StatusIcon = Wifi;
  let statusText = "Povezano";

  if (isError) {
    statusColor = "bg-red-500";
    StatusIcon = WifiOff;
    statusText = "Greška u vezi";
  } else if (isStale) {
    statusColor = "bg-yellow-500";
    StatusIcon = AlertTriangle;
    statusText = "Zastarjeli podaci";
  } else if (isLoading && !lastUpdated) {
    statusColor = "bg-blue-500";
    StatusIcon = Clock;
    statusText = "Povezivanje...";
  }

  return (
    <div className="absolute top-4 left-4 right-4 z-[1000] pointer-events-none flex justify-center">
      <div className="bg-background/80 backdrop-blur-xl border border-white/10 shadow-2xl rounded-2xl px-4 py-3 flex flex-wrap items-center gap-4 pointer-events-auto max-w-2xl w-full justify-between md:justify-center animate-fade-in">
        
        {/* Connection Status */}
        <div className="flex items-center gap-2">
          <div className="relative flex h-3 w-3">
            {!isError && !isStale && (
              <span className={cn("animate-ping absolute inline-flex h-full w-full rounded-full opacity-75", statusColor)}></span>
            )}
            <span className={cn("relative inline-flex rounded-full h-3 w-3", statusColor)}></span>
          </div>
          <span className="text-sm font-medium text-foreground hidden sm:inline-block">{statusText}</span>
        </div>

        <div className="w-px h-4 bg-white/20 hidden sm:block"></div>

        {/* Count */}
        <div className="flex items-center gap-1.5">
          <span className="text-xl font-display font-bold text-foreground">{vehicleCount}</span>
          <span className="text-sm text-muted-foreground font-medium">autobusa</span>
        </div>

        <div className="w-px h-4 bg-white/20 hidden sm:block"></div>

        {/* Time */}
        <div className="flex items-center gap-1.5 text-muted-foreground">
          <Clock className="w-4 h-4" />
          <span className="text-sm font-medium font-mono tracking-tight">{formattedTime}</span>
        </div>

        {/* Demo Badge */}
        {source === "demo" && (
          <>
            <div className="w-px h-4 bg-white/20"></div>
            <div className="bg-primary/20 text-primary border border-primary/30 px-2 py-0.5 rounded-md text-xs font-bold tracking-wider uppercase">
              Demo Mod
            </div>
          </>
        )}
      </div>
    </div>
  );
}
