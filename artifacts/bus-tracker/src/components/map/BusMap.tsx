import { useState, useMemo, useCallback, useEffect } from "react";
import { MapContainer, TileLayer, Marker, ZoomControl, Polyline, CircleMarker, useMap } from "react-leaflet";
import MarkerClusterGroup from "react-leaflet-cluster";
import L from "leaflet";
import { Sun, Moon, Loader2 } from "lucide-react";
import type { Vehicle } from "@workspace/api-client-react";
import { getStatusColor } from "@/lib/utils";
import { VehicleDetails } from "./VehicleDetails";
import { useRouteData } from "@/hooks/use-route-data";

interface BusMapProps {
  vehicles: Vehicle[];
}

const DEFAULT_CENTER: [number, number] = [43.513, 16.45];
const DEFAULT_ZOOM = 13;

const TILE_LAYERS = {
  dark: {
    url: "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png",
    attribution: '&copy; <a href="https://carto.com/">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    label: "Tamna",
  },
  light: {
    url: "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png",
    attribution: '&copy; <a href="https://carto.com/">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    label: "Lagana",
  },
};

const createClusterCustomIcon = function (cluster: L.MarkerCluster) {
  const count = cluster.getChildCount();
  return L.divIcon({
    html: `<div class="flex items-center justify-center w-full h-full font-bold text-white text-sm">${count}</div>`,
    className: "custom-cluster",
    iconSize: L.point(40, 40, true),
  });
};

// Component to pan map when a vehicle is selected
function MapPanner({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap();
  useEffect(() => {
    map.panTo([lat, lng], { animate: true, duration: 0.5 });
  }, [lat, lng, map]);
  return null;
}

// Route overlay component
function RouteLayer({ lineName, color }: { lineName: string; color: string }) {
  const { data, isLoading } = useRouteData(lineName);

  if (isLoading || !data) return null;

  return (
    <>
      {data.routes.map((segment, i) => (
        <Polyline
          key={i}
          positions={segment}
          pathOptions={{
            color,
            weight: 4,
            opacity: 0.75,
            dashArray: undefined,
          }}
        />
      ))}
      {data.stops
        .filter((s) => s.role === "stop" || s.role === "stop_entry_only" || s.role === "stop_exit_only" || s.role === "platform")
        .map((stop, i) => (
          <CircleMarker
            key={i}
            center={[stop.lat, stop.lon]}
            radius={5}
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
  const [tileMode, setTileMode] = useState<"dark" | "light">("dark");

  const selectedVehicle = useMemo(
    () => vehicles.find((v) => v.id === selectedVehicleId) || null,
    [vehicles, selectedVehicleId]
  );

  const handleClose = useCallback(() => setSelectedVehicleId(null), []);
  const toggleTile = useCallback(() => setTileMode((m) => (m === "dark" ? "light" : "dark")), []);

  // Is route loading?
  const { isLoading: isRouteLoading } = useRouteData(selectedVehicle?.name ?? null);

  const tile = TILE_LAYERS[tileMode];

  const markers = useMemo(() => {
    return vehicles.map((vehicle) => {
      const isSelected = vehicle.id === selectedVehicleId;
      const isSameLine = selectedVehicle && vehicle.name === selectedVehicle.name;
      const color = getStatusColor(vehicle.vehicleStatus);

      const opacity = selectedVehicle && !isSameLine ? 0.35 : 1;
      const scale = isSelected ? 1.3 : isSameLine ? 1.1 : 1;

      const html = `
        <div style="opacity:${opacity};transform:scale(${scale});transition:all 0.2s ease;transform-origin:center;width:32px;height:32px;position:relative;">
          <div style="
            position:absolute;inset:0;border-radius:50%;
            background:${color};
            border:2px solid ${isSelected ? 'white' : 'rgba(255,255,255,0.7)'};
            box-shadow:0 2px 8px rgba(0,0,0,0.4)${isSelected ? ',0 0 0 3px rgba(255,255,255,0.3)' : ''};
          "></div>
          <span style="
            position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
            color:white;font-weight:700;font-size:${vehicle.name.length > 2 ? '9' : '11'}px;
            font-family:system-ui,sans-serif;text-shadow:0 1px 2px rgba(0,0,0,0.5);
          ">${vehicle.name}</span>
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
  }, [vehicles, selectedVehicleId, selectedVehicle]);

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

        {/* Zoom controls — bottom right */}
        <ZoomControl position="bottomright" />

        {/* Route overlay when vehicle selected */}
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

        {/* Pan to selected vehicle */}
        {selectedVehicle && (
          <MapPanner lat={selectedVehicle.latitude} lng={selectedVehicle.longitude} />
        )}
      </MapContainer>

      {/* Map style toggle button */}
      <button
        onClick={toggleTile}
        className="absolute bottom-20 right-3 z-[1000] w-10 h-10 flex items-center justify-center rounded-lg shadow-lg border border-white/20 backdrop-blur-md transition-all hover:scale-105 active:scale-95"
        style={{ background: tileMode === "dark" ? "rgba(30,30,40,0.9)" : "rgba(255,255,255,0.9)" }}
        title={tileMode === "dark" ? "Lagana karta" : "Tamna karta"}
      >
        {tileMode === "dark"
          ? <Sun className="w-5 h-5 text-yellow-300" />
          : <Moon className="w-5 h-5 text-slate-700" />
        }
      </button>

      {/* Route loading indicator */}
      {selectedVehicle && isRouteLoading && (
        <div className="absolute top-20 left-1/2 -translate-x-1/2 z-[1100] bg-background/80 backdrop-blur-md border border-white/10 rounded-full px-3 py-1.5 flex items-center gap-2 text-xs text-muted-foreground">
          <Loader2 className="w-3 h-3 animate-spin" />
          Učitavanje trase linije {selectedVehicle.name}...
        </div>
      )}

      <VehicleDetails
        vehicle={selectedVehicle}
        onClose={handleClose}
      />
    </div>
  );
}
