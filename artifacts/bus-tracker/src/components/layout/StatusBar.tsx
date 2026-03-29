import { useState } from "react";
import { AlertTriangle, Info, X } from "lucide-react";
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

const DISCLAIMER = "Podaci su preuzeti s web stranica Promet Split i ne garantiramo za njihovu točnost.";

const LEGEND = [
  { color: "#22c55e", label: "U vožnji" },
  { color: "#f97316", label: "Polazi" },
  { color: "#a855f7", label: "Na stanici" },
  { color: "#9ca3af", label: "Miruje" },
] as const;

export function StatusBar({ isLoading, isError, isStale, lastUpdated, vehicleCount, source }: StatusBarProps) {
  const formattedTime = lastUpdated ? format(lastUpdated, "HH:mm:ss") : "--:--:--";
  const [showTooltip, setShowTooltip] = useState(false);

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

        {/* Legend + info */}
        <div className="mt-1.5 pt-1.5 border-t border-black/[0.06] dark:border-white/[0.08] flex items-center gap-2 flex-wrap">
          {LEGEND.map(({ color, label }) => (
            <div key={label} className="flex items-center gap-1">
              <span
                className="inline-block w-2 h-2 rounded-full shrink-0"
                style={{ backgroundColor: color }}
              />
              <span className="text-[10px] text-gray-500 dark:text-gray-400">{label}</span>
            </div>
          ))}

          {/* Info button */}
          <button
            onClick={() => setShowTooltip(!showTooltip)}
            className="ml-auto shrink-0 w-4 h-4 rounded-full flex items-center justify-center text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
            aria-label="Informacije o podacima"
          >
            <Info className="w-3 h-3" />
          </button>
        </div>

        {/* Info tooltip */}
        {showTooltip && (
          <div className="mt-1.5 pt-1.5 border-t border-black/[0.06] dark:border-white/[0.08]">
            <p className="text-[10px] leading-snug text-gray-500 dark:text-gray-400 pr-4">
              {DISCLAIMER}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

/* ── First-visit disclaimer modal ── */

const DISMISSED_KEY = "bus-tracker-disclaimer-seen";

export function DisclaimerModal() {
  const [visible, setVisible] = useState(() => {
    try { return !localStorage.getItem(DISMISSED_KEY); }
    catch { return true; }
  });

  if (!visible) return null;

  const dismiss = () => {
    try { localStorage.setItem(DISMISSED_KEY, "1"); }
    catch { /* private browsing */ }
    setVisible(false);
  };

  return (
    <div
      className="fixed inset-0 z-[2000] flex items-end sm:items-center justify-center p-4"
      onClick={dismiss}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]" />

      {/* Card */}
      <div
        className={cn(
          "relative w-full max-w-sm rounded-2xl overflow-hidden",
          "bg-white/90 dark:bg-gray-900/90 backdrop-blur-xl",
          "border border-black/[0.06] dark:border-white/[0.08]",
          "shadow-xl",
          "animate-fade-in"
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 pt-5 pb-4">
          {/* Icon + close */}
          <div className="flex items-start justify-between mb-3">
            <div className="w-9 h-9 rounded-xl bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center">
              <Info className="w-4.5 h-4.5 text-blue-500 dark:text-blue-400" />
            </div>
            <button
              onClick={dismiss}
              className="w-7 h-7 -mr-1 -mt-1 rounded-lg flex items-center justify-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
              aria-label="Zatvori"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <p className="text-sm leading-relaxed text-gray-600 dark:text-gray-300">
            {DISCLAIMER}
          </p>
        </div>

        <button
          onClick={dismiss}
          className={cn(
            "w-full py-3 text-sm font-medium",
            "border-t border-black/[0.06] dark:border-white/[0.08]",
            "text-blue-600 dark:text-blue-400",
            "hover:bg-black/[0.03] dark:hover:bg-white/[0.04]",
            "active:bg-black/[0.06] dark:active:bg-white/[0.08]",
            "transition-colors"
          )}
        >
          Razumijem
        </button>
      </div>
    </div>
  );
}
