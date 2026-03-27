import { AlertTriangle } from "lucide-react";
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

  const hasWarning = isError || isStale;

  return (
    <div className="absolute top-3 left-3 z-[1000] pointer-events-none">
      <div
        className={cn(
          "pointer-events-auto rounded-lg px-3 py-2 text-xs leading-relaxed",
          "bg-white/70 dark:bg-black/50 backdrop-blur-md",
          "border border-black/[0.06] dark:border-white/[0.08]",
          "shadow-sm",
          "transition-all duration-300"
        )}
      >
        {/* Vehicle count */}
        <div className="flex items-center gap-1.5 text-gray-700 dark:text-gray-200">
          <span className="font-normal text-gray-500 dark:text-gray-400">Autobusa:</span>
          <span className="font-semibold tabular-nums">{vehicleCount}</span>
          {source === "demo" && (
            <span className="ml-1 text-[10px] uppercase tracking-wide text-amber-600 dark:text-amber-400 font-medium">
              demo
            </span>
          )}
        </div>

        {/* Last updated */}
        <div className="flex items-center gap-1.5 text-gray-700 dark:text-gray-200 mt-0.5">
          <span className="font-normal text-gray-500 dark:text-gray-400">Ažurirano:</span>
          <span className="font-semibold tabular-nums tracking-tight">{formattedTime}</span>

          {hasWarning && (
            <AlertTriangle className="w-3 h-3 text-amber-500 ml-0.5 shrink-0" />
          )}
        </div>
      </div>
    </div>
  );
}
