import { useState, useMemo, useCallback, useEffect } from "react";
import { MapContainer, TileLayer, Marker, ZoomControl, Polyline, CircleMarker, useMap } from "react-leaflet";
import MarkerClusterGroup from "react-leaflet-cluster";
import L from "leaflet";
import { Loader2, Layers } from "lucide-react";
import type { Vehicle } from "@workspace/api-client-react";
import { getStatusColor } from "@/lib/utils";
import { VehicleDetails } from "./VehicleDetails";
import { useRouteData } from "@/hooks/use-route-data";

interface BusMapProps {
  vehicles: Vehicle[];
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

const TILE_ICONS: Record<TileMode, { next: string; bg: string; text: string }> = {
  dark: { next: "Lagana", bg: "rgba(20,21,35,0.92)", text: "#fff" },
  light: { next: "OSM",    bg: "rgba(255,255,255,0.92)", text: "#333" },
  osm:  { next: "Tamna",  bg: "rgba(240,240,240,0.92)", text: "#333" },
};

const createClusterCustomIcon = function (cluster: L.MarkerCluster) {
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

function RouteLayer({ lineName, color }: { lineName: string; color: string }) {
  const { data } = useRouteData(lineName);

  if (!data || data.routes.length === 0) return null;

  const isApproximate = data.source === "osrm";

  return (
    <>
      {data.routes.map((segment, i) => (
        <Polyline
          key={i}
          positions={segment}
          pathOptions={{
            color,
            weight: isApproximate ? 3 : 4,
            opacity: isApproximate ? 0.55 : 0.8,
            dashArray: isApproximate ? "8 6" : undefined,
          }}
        />
      ))}
      {data.stops
        .filter((s) => ["stop", "stop_entry_only", "stop_exit_only", "platform"].includes(s.role))
        .map((stop, i) => (
          <CircleMarker
            key={i}
            center={[stop.lat, stop.lon]}
            radius={isApproximate ? 4 : 5}
            pathOptions={{
              color: "white",
              fillColor: color,
              fillOpacity: 1,
              weight: 2,
            }}
          />
        ))}
    </>
  );
}

export function BusMap({ vehicles }: BusMapProps) {
  const [selectedVehicleId, setSelectedVehicleId] = useState<number | null>(null);
  const [tileMode, setTileMode] = useState<TileMode>("dark");
  const [showTilePicker, setShowTilePicker] = useState(false);

  const selectedVehicle = useMemo(
    () => vehicles.find((v) => v.id === selectedVehicleId) || null,
    [vehicles, selectedVehicleId]
  );

  const handleClose = useCallback(() => setSelectedVehicleId(null), []);

  const { isLoading: isRouteLoading, data: routeData } = useRouteData(selectedVehicle?.name ?? null);

  const tile = TILE_LAYERS[tileMode];
  const tileIcon = TILE_ICONS[tileMode];
  const isLightMap = tileMode !== "dark";

  const markers = useMemo(() => {
    return vehicles.map((vehicle) => {
      const isSelected = vehicle.id === selectedVehicleId;
      const isSameLine = !!(selectedVehicle && vehicle.name === selectedVehicle.name);
      const color = getStatusColor(vehicle.vehicleStatus);

      const opacity = selectedVehicle && !isSameLine ? 0.3 : 1;
      const scale = isSelected ? 1.3 : isSameLine ? 1.1 : 1;
      const borderColor = isSelected ? "white" : "rgba(255,255,255,0.75)";

      const html = `
        <div style="opacity:${opacity};transform:scale(${scale});transition:all 0.2s ease;transform-origin:center;width:32px;height:32px;position:relative;">
          <div style="position:absolute;inset:0;border-radius:50%;background:${color};border:2px solid ${borderColor};box-shadow:0 2px 8px rgba(0,0,0,0.45)${isSelected ? ",0 0 0 3px rgba(255,255,255,0.25)" : ""};"></div>
          <span style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:white;font-weight:700;font-size:${vehicle.name.length > 2 ? "9" : "11"}px;font-family:system-ui,sans-serif;text-shadow:0 1px 2px rgba(0,0,0,0.6);">${vehicle.name}</span>
        </div>
      `;

      const icon = L.divIcon({
        html,
        className: "bg-transparent border-0",
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      return (
        <Marker
          key={vehicle.id}
          position={[vehicle.latitude, vehicle.longitude]}
          icon={icon}
          eventHandlers={{
            click: () => setSelectedVehicleId((prev) => (prev === vehicle.id ? null : vehicle.id)),
          }}
          zIndexOffset={isSelected ? 2000 : isSameLine ? 1000 : 0}
        />
      );
    });
  }, [vehicles, selectedVehicleId, selectedVehicle, isLightMap]);

  return (
    <div className="relative w-full h-screen bg-black overflow-hidden z-0">
      <MapContainer
        center={DEFAULT_CENTER}
        zoom={DEFAULT_ZOOM}
        zoomControl={false}
        className="w-full h-full z-0"
      >
        <TileLayer
          key={tileMode}
          attribution={tile.attribution}
          url={tile.url}
          maxZoom={19}
        />

        {/* Zoom controls — bottom right, above filter bar */}
        <ZoomControl position="bottomright" />

        {selectedVehicle && (
          <RouteLayer
            lineName={selectedVehicle.name}
            color={getStatusColor(selectedVehicle.vehicleStatus)}
          />
        )}

        <MarkerClusterGroup
          chunkedLoading
          iconCreateFunction={createClusterCustomIcon}
          showCoverageOnHover={false}
          maxClusterRadius={50}
          disableClusteringAtZoom={15}
        >
          {markers}
        </MarkerClusterGroup>

        {selectedVehicle && (
          <MapPanner lat={selectedVehicle.latitude} lng={selectedVehicle.longitude} />
        )}
      </MapContainer>

      {/* Map layer picker — above zoom controls, which end at ~168px from bottom */}
      <div className="absolute bottom-[185px] right-3 z-[1000] flex flex-col items-end gap-1.5">
        {/* Expanded tile options */}
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
                  borderColor: tileMode === mode ? (mode === "dark" ? "rgba(255,255,255,0.4)" : "rgba(0,0,0,0.3)") : "transparent",
                  boxShadow: tileMode === mode ? "0 0 0 2px #60a5fa" : undefined,
                }}
              >
                {TILE_LAYERS[mode].label}
                {tileMode === mode && <span className="opacity-50 text-[10px]">✓</span>}
              </button>
            ))}
          </div>
        )}

        {/* Layers toggle button */}
        <button
          onClick={() => setShowTilePicker((v) => !v)}
          className="w-10 h-10 flex items-center justify-center rounded-lg shadow-lg border transition-all hover:scale-105 active:scale-95"
          style={{
            background: tileIcon.bg,
            borderColor: "rgba(128,128,128,0.2)",
          }}
          title="Izmjeni kartu"
        >
          <Layers className="w-5 h-5" style={{ color: tileIcon.text }} />
        </button>
      </div>

      {/* Route loading / source indicator */}
      {selectedVehicle && (isRouteLoading || (routeData && routeData.source !== "none")) && (
        <div className="absolute top-20 left-1/2 -translate-x-1/2 z-[1100] bg-background/85 backdrop-blur-md border border-white/10 rounded-full px-3 py-1.5 flex items-center gap-2 text-xs text-muted-foreground whitespace-nowrap">
          {isRouteLoading ? (
            <>
              <Loader2 className="w-3 h-3 animate-spin" />
              Učitavanje trase {selectedVehicle.name}...
            </>
          ) : routeData?.source === "osm" ? (
            <span className="text-green-400">● Trasa: OSM podaci</span>
          ) : routeData?.source === "osrm" ? (
            <span className="text-yellow-400">● Trasa: aproksimacija (stvarne ceste)</span>
          ) : null}
        </div>
      )}

      <VehicleDetails
        vehicle={selectedVehicle}
        onClose={handleClose}
      />
    </div>
  );
}
