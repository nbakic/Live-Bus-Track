import { useEffect, useState } from "react";
import { X, MapPin, Loader2 } from "lucide-react";

interface StopInfo {
  name?: string;
  address?: string;
  road?: string;
}

interface StopInfoPanelProps {
  stop: { lat: number; lon: number } | null;
  lineColor: string;
  onClose: () => void;
}

function useStopAddress(lat: number | null, lon: number | null) {
  const [info, setInfo] = useState<StopInfo | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (lat == null || lon == null) { setInfo(null); return; }
    setLoading(true);
    setInfo(null);
    const ctrl = new AbortController();
    fetch(
      `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&accept-language=hr`,
      { signal: ctrl.signal, headers: { "Accept-Language": "hr" } }
    )
      .then((r) => r.json())
      .then((d) => {
        const addr = d.address || {};
        const name =
          d.name && d.name !== addr.road ? d.name : addr.amenity || addr.leisure || undefined;
        const road = addr.road
          ? `${addr.road}${addr.house_number ? ` ${addr.house_number}` : ""}`
          : undefined;
        const address =
          road ||
          (d.display_name ? d.display_name.split(",").slice(0, 2).join(", ") : undefined);
        setInfo({ name, address });
      })
      .catch(() => {})
      .finally(() => setLoading(false));
    return () => ctrl.abort();
  }, [lat, lon]);

  return { info, loading };
}

export function StopInfoPanel({ stop, lineColor, onClose }: StopInfoPanelProps) {
  const { info, loading } = useStopAddress(stop?.lat ?? null, stop?.lon ?? null);

  if (!stop) return null;

  return (
    <div className="absolute bottom-24 left-4 right-4 z-[2100] pointer-events-none flex justify-center">
      <div className="bg-background/95 backdrop-blur-2xl border border-white/10 shadow-2xl rounded-2xl p-4 pointer-events-auto w-full max-w-sm relative overflow-hidden">
        <div className="absolute top-0 left-0 right-0 h-1" style={{ backgroundColor: lineColor }} />

        <button
          onClick={onClose}
          className="absolute top-3 right-3 p-1.5 bg-white/5 hover:bg-white/10 rounded-full text-muted-foreground hover:text-foreground transition-colors"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="flex items-start gap-3 pr-6 pt-1">
          <div
            className="flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center border-2 border-white"
            style={{ backgroundColor: lineColor }}
          >
            <MapPin className="w-4 h-4 text-white" />
          </div>

          <div className="flex-1 min-w-0">
            {loading ? (
              <div className="flex items-center gap-2 text-muted-foreground text-sm">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Učitavanje adrese…
              </div>
            ) : (
              <>
                {info?.name && (
                  <p className="font-semibold text-foreground leading-tight truncate">{info.name}</p>
                )}
                <p className="text-sm text-muted-foreground leading-snug">
                  {info?.address ?? `${stop.lat.toFixed(5)}, ${stop.lon.toFixed(5)}`}
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
