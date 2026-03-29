import { useState, useMemo, useCallback, useEffect, Fragment } from "react";
import {
  MapContainer, TileLayer, Marker, ZoomControl,
  Polyline, CircleMarker, useMap,
} from "react-leaflet";
import MarkerClusterGroup from "react-leaflet-cluster";
import L from "leaflet";
import { Loader2, Layers } from "lucide-react";
import type { Vehicle } from "@workspace/api-client-react";
import { getStatusColor } from "@/lib/utils";
import { VehicleDetails } from "./VehicleDetails";
import { StopInfoPanel } from "./StopInfoPanel";
import { StopsListModal } from "./StopsListModal";
import { useRouteData } from "@/hooks/use-route-data";

interface BusMapProps {
  vehicles: Vehicle[];
}

interface StopPoint {
  lat: number;
  lon: number;
  role: string;
}

const DEFAULT_CENTER: [number, number] = [43.513, 16.45];
const DEFAULT_ZOOM = 13;

type TileMode = "dark" | "light" | "osm";

const TILE_LAYERS: Record<TileMode, { url: string; attribution: string; label: string }> = {
  dark: {
    url: "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png",
    attribution: '&copy; <a href="https://carto.com/">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>',
    label: "Tamna",
  },
  light: {
    url: "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png",
    attribution: '&copy; <a href="https://carto.com/">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>',
    label: "Lagana",
  },
  osm: {
    url: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    label: "OSM",
  },
};

const TILE_ORDER: TileMode[] = ["dark", "light", "osm"];
const TILE_ICONS: Record<TileMode, { bg: string; text: string }> = {
  dark:  { bg: "rgba(20,21,35,0.92)",    text: "#fff" },
  light: { bg: "rgba(255,255,255,0.92)", text: "#333" },
  osm:   { bg: "rgba(240,240,240,0.92)", text: "#333" },
};

const createClusterCustomIcon = (cluster: any) => {
  const count = cluster.getChildCount();
  return L.divIcon({
    html: `<div style="display:flex;align-items:center;justify-content:center;width:100%;height:100%;font-weight:700;font-size:13px;color:white;">${count}</div>`,
    className: "custom-cluster",
    iconSize: L.point(40, 40, true),
  });
};

function MapPanner({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap();
  useEffect(() => {
    map.panTo([lat, lng], { animate: true, duration: 0.5 });
  }, [lat, lng, map]);
  return null;
}

// ---- Route Layer ----
function RouteLayer({
  data,
  color,
  onStopClick,
  onLineClick,
}: {
  data: ReturnType<typeof useRouteData>["data"];
  color: string;
  onStopClick: (stop: StopPoint) => void;
  onLineClick: () => void;
}) {
  if (!data || data.routes.length === 0) return null;
  const isApproximate = data.source === "osrm";
  const stops = data.stops.filter((s) =>
    ["stop", "stop_entry_only", "stop_exit_only", "platform"].includes(s.role)
  );

  return (
    <>
      {data.routes.map((segment, i) => (
        <Fragment key={i}>
          <Polyline
            positions={segment}
            pathOptions={{ color: "rgba(0,0,0,0.4)", weight: isApproximate ? 11 : 13, opacity: 1, lineCap: "round", lineJoin: "round", dashArray: isApproximate ? "12 8" : undefined }}
            eventHandlers={{ click: (e) => { L.DomEvent.stopPropagation(e); onLineClick(); } }}
          />
          <Polyline
            positions={segment}
            pathOptions={{ color, weight: isApproximate ? 6 : 8, opacity: 1, lineCap: "round", lineJoin: "round", dashArray: isApproximate ? "12 8" : undefined }}
            eventHandlers={{ click: (e) => { L.DomEvent.stopPropagation(e); onLineClick(); } }}
          />
        </Fragment>
      ))}

      {stops.map((stop, i) => (
        <CircleMarker
          key={i}
          center={[stop.lat, stop.lon]}
          radius={isApproximate ? 5 : 7}
          pathOptions={{ color: "white", fillColor: color, fillOpacity: 1, weight: 2.5 }}
          eventHandlers={{
            click: (e) => { L.DomEvent.stopPropagation(e); onStopClick(stop); },
          }}
        />
      ))}
    </>
  );
}

// ---- Main Map ----
export function BusMap({ vehicles }: BusMapProps) {
  const [selectedVehicleId, setSelectedVehicleId] = useState<number | null>(null);
  const [tileMode, setTileMode] = useState<TileMode>("osm");
  const [showTilePicker, setShowTilePicker] = useState(false);
  const [clickedStop, setClickedStop] = useState<StopPoint | null>(null);
  const [showStopsList, setShowStopsList] = useState(false);

  const selectedVehicle = useMemo(
    () => vehicles.find((v) => v.id === selectedVehicleId) || null,
    [vehicles, selectedVehicleId]
  );

  const handleCloseVehicle = useCallback(() => setSelectedVehicleId(null), []);

  const { isLoading: isRouteLoading, data: routeData } = useRouteData(
    selectedVehicle?.name ?? null
  );

  const lineColor = "#6366f1";

  const tile = TILE_LAYERS[tileMode];
  const tileIcon = TILE_ICONS[tileMode];

  // Close stop info / stops list on vehicle deselect
  useEffect(() => {
    if (!selectedVehicle) {
      setClickedStop(null);
      setShowStopsList(false);
    }
  }, [selectedVehicle]);

  const markers = useMemo(() => {
    return vehicles.map((vehicle) => {
      const isSelected = vehicle.id === selectedVehicleId;
      const isSameLine = !!(selectedVehicle && vehicle.name === selectedVehicle.name);
      const color = getStatusColor(vehicle.vehicleStatus);

      const opacity = selectedVehicle && !isSameLine ? 0.55 : 1;
      const size = isSelected ? 46 : isSameLine ? 42 : 38;
      const fontSize = vehicle.name.length > 2 ? "11" : "14";
      const shadow = isSelected
        ? `0 3px 12px rgba(0,0,0,0.6), 0 0 0 3px rgba(255,255,255,0.4), 0 0 16px ${color}88`
        : `0 3px 10px rgba(0,0,0,0.55), 0 1px 0 rgba(255,255,255,0.15)`;

      const html = `
        <div style="opacity:${opacity};transition:opacity 0.2s ease;width:${size}px;height:${size}px;position:relative;filter:drop-shadow(0 2px 4px rgba(0,0,0,0.5));">
          <div style="position:absolute;inset:0;border-radius:50%;background:${color};border:3px solid white;box-shadow:${shadow};"></div>
          <span style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:white;font-weight:900;font-size:${fontSize}px;font-family:system-ui,sans-serif;letter-spacing:-0.5px;text-shadow:0 1px 4px rgba(0,0,0,0.8);">${vehicle.name}</span>
        </div>
      `;

      const icon = L.divIcon({
        html,
        className: "!bg-transparent !border-0 !shadow-none",
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
      });

      return (
        <Marker
          key={vehicle.id}
          position={[vehicle.latitude, vehicle.longitude]}
          icon={icon}
          eventHandlers={{
            click: () => {
              setClickedStop(null);
              setShowStopsList(false);
              setSelectedVehicleId((prev) => (prev === vehicle.id ? null : vehicle.id));
            },
          }}
          zIndexOffset={isSelected ? 2000 : isSameLine ? 1000 : 0}
        />
      );
    });
  }, [vehicles, selectedVehicleId, selectedVehicle]);

  const handleStopClick = useCallback((stop: StopPoint) => {
    setClickedStop(stop);
    setShowStopsList(false);
  }, []);

  const handleLineClick = useCallback(() => {
    setShowStopsList(true);
    setClickedStop(null);
  }, []);

  const anyPanelOpen = !!(clickedStop || showStopsList);

  return (
    <div className="relative w-full h-screen bg-black overflow-hidden z-0">
      <MapContainer
        center={DEFAULT_CENTER}
        zoom={DEFAULT_ZOOM}
        zoomControl={false}
        className="w-full h-full z-0"
      >
        <TileLayer key={tileMode} attribution={tile.attribution} url={tile.url} maxZoom={19} />

        <ZoomControl position="bottomright" />

        {selectedVehicle && routeData && (
          <RouteLayer
            data={routeData}
            color={lineColor}
            onStopClick={handleStopClick}
            onLineClick={handleLineClick}
          />
        )}

        <MarkerClusterGroup
          chunkedLoading
          iconCreateFunction={createClusterCustomIcon}
          showCoverageOnHover={false}
          maxClusterRadius={30}
          disableClusteringAtZoom={14}
        >
          {markers}
        </MarkerClusterGroup>

        {selectedVehicle && (
          <MapPanner lat={selectedVehicle.latitude} lng={selectedVehicle.longitude} />
        )}
      </MapContainer>

      {/* Layer picker */}
      <div className="absolute bottom-[280px] right-3 z-[1000] flex flex-col items-end gap-1.5">
        {showTilePicker && (
          <div className="flex flex-col gap-1.5 items-end">
            {TILE_ORDER.map((mode) => (
              <button
                key={mode}
                onClick={() => { setTileMode(mode); setShowTilePicker(false); }}
                className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold shadow-lg border transition-all"
                style={{
                  background: mode === "dark" ? "rgba(20,21,35,0.95)" : "rgba(255,255,255,0.95)",
                  color: mode === "dark" ? "#fff" : "#222",
                  borderColor: tileMode === mode
                    ? (mode === "dark" ? "rgba(255,255,255,0.4)" : "rgba(0,0,0,0.3)")
                    : "transparent",
                  boxShadow: tileMode === mode ? "0 0 0 2px #60a5fa" : undefined,
                }}
              >
                {TILE_LAYERS[mode].label}
                {tileMode === mode && <span className="opacity-50 text-[10px] ml-0.5">✓</span>}
              </button>
            ))}
          </div>
        )}
        <button
          onClick={() => setShowTilePicker((v) => !v)}
          className="w-10 h-10 flex items-center justify-center rounded-lg shadow-lg border transition-all hover:scale-105 active:scale-95"
          style={{ background: tileIcon.bg, borderColor: "rgba(128,128,128,0.2)" }}
          title="Izmjeni kartu"
        >
          <Layers className="w-5 h-5" style={{ color: tileIcon.text }} />
        </button>
      </div>

      {/* Route loading indicator */}
      {selectedVehicle && (isRouteLoading || (routeData && routeData.source !== "none")) && !anyPanelOpen && (
        <div className="absolute top-20 left-1/2 -translate-x-1/2 z-[1100] bg-black/70 backdrop-blur-md border border-white/15 rounded-full px-3 py-1.5 flex items-center gap-2 text-xs whitespace-nowrap">
          {isRouteLoading ? (
            <>
              <Loader2 className="w-3 h-3 animate-spin text-white/60" />
              <span className="text-white/70">Trasa {selectedVehicle.name}…</span>
            </>
          ) : routeData?.source === "osm" ? (
            <span className="text-emerald-400 font-medium">● OSM trasa</span>
          ) : routeData?.source === "osrm" ? (
            <span className="text-amber-400 font-medium">● Aproksimacija trase</span>
          ) : null}
        </div>
      )}

      {/* Stop info panel — appears above vehicle details if both open */}
      {clickedStop && !showStopsList && (
        <StopInfoPanel
          stop={clickedStop}
          lineColor={lineColor}
          onClose={() => setClickedStop(null)}
        />
      )}

      {/* Stops list modal */}
      {showStopsList && selectedVehicle && (
        <StopsListModal
          lineName={selectedVehicle.name}
          lineColor={lineColor}
          routeData={routeData}
          onClose={() => setShowStopsList(false)}
          onStopSelect={(stop) => setClickedStop({ ...stop, role: "stop" })}
        />
      )}

      {/* Vehicle details — hidden when stops list is open */}
      {!showStopsList && (
        <VehicleDetails vehicle={selectedVehicle} onClose={handleCloseVehicle} />
      )}
    </div>
  );
}
