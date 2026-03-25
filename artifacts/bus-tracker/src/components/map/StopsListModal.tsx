import { X, MapPin } from "lucide-react";
import type { RouteData } from "@/hooks/use-route-data";

interface StopsListModalProps {
  lineName: string;
  lineColor: string;
  routeData: RouteData | undefined;
  onClose: () => void;
  onStopSelect?: (stop: { lat: number; lon: number }) => void;
}

export function StopsListModal({
  lineName,
  lineColor,
  routeData,
  onClose,
  onStopSelect,
}: StopsListModalProps) {
  const stops = (routeData?.stops ?? []).filter((s) =>
    ["stop", "stop_entry_only", "stop_exit_only", "platform"].includes(s.role)
  );

  return (
    <>
      {/* Backdrop */}
      <div
        className="absolute inset-0 z-[2050] bg-black/40 backdrop-blur-[2px]"
        onClick={onClose}
      />

      {/* Modal — slides up from bottom, mobile-friendly */}
      <div className="absolute bottom-0 left-0 right-0 z-[2100] pointer-events-auto flex flex-col max-h-[80vh] animate-slide-up">
        <div className="bg-background/98 backdrop-blur-2xl border-t border-x border-white/10 shadow-2xl rounded-t-3xl flex flex-col overflow-hidden">

          {/* Handle + Header */}
          <div className="flex-shrink-0 px-5 pt-4 pb-3 border-b border-white/8">
            {/* Drag handle */}
            <div className="w-10 h-1 rounded-full bg-white/20 mx-auto mb-4" />

            <div className="flex items-center gap-3">
              <div
                className="w-10 h-10 rounded-xl flex items-center justify-center font-black text-white text-sm flex-shrink-0"
                style={{ backgroundColor: lineColor }}
              >
                {lineName}
              </div>
              <div className="flex-1">
                <p className="font-bold text-foreground leading-tight">Linija {lineName}</p>
                <p className="text-xs text-muted-foreground">
                  {stops.length > 0 ? `${stops.length} stanica` : "Nema podataka o stanicama"}
                </p>
              </div>
              <button
                onClick={onClose}
                className="p-2 bg-white/8 hover:bg-white/15 rounded-full text-muted-foreground hover:text-foreground transition-colors flex-shrink-0"
                aria-label="Zatvori"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Stops list — scrollable */}
          <div className="overflow-y-auto overscroll-contain flex-1">
            {stops.length === 0 ? (
              <div className="px-5 py-10 text-center text-muted-foreground text-sm">
                Podaci o stanicama nisu dostupni za ovu liniju.
              </div>
            ) : (
              <ul className="py-2">
                {stops.map((stop, i) => (
                  <li key={i}>
                    <button
                      className="w-full flex items-center gap-4 px-5 py-3 hover:bg-white/5 active:bg-white/8 transition-colors text-left"
                      onClick={() => {
                        onStopSelect?.(stop);
                        onClose();
                      }}
                    >
                      {/* Step number + line connector */}
                      <div className="flex flex-col items-center flex-shrink-0 w-8">
                        <div
                          className="w-7 h-7 rounded-full flex items-center justify-center text-white font-bold text-[11px] border-2 border-white"
                          style={{ backgroundColor: lineColor }}
                        >
                          {i + 1}
                        </div>
                        {i < stops.length - 1 && (
                          <div
                            className="w-0.5 h-4 mt-1 rounded-full opacity-30"
                            style={{ backgroundColor: lineColor }}
                          />
                        )}
                      </div>

                      {/* Stop info */}
                      <div className="flex-1 min-w-0 py-0.5">
                        <p className="text-sm font-medium text-foreground leading-tight">
                          Stanica {i + 1}
                        </p>
                        <p className="text-xs text-muted-foreground font-mono mt-0.5">
                          {stop.lat.toFixed(4)}, {stop.lon.toFixed(4)}
                        </p>
                      </div>

                      <MapPin className="w-4 h-4 text-muted-foreground/50 flex-shrink-0" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Bottom safe area for mobile notch */}
          <div className="flex-shrink-0 h-6" />
        </div>
      </div>
    </>
  );
}
