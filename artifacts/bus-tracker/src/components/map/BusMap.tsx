import { useState, useMemo } from "react";
import { MapContainer, TileLayer, Marker } from "react-leaflet";
import MarkerClusterGroup from "react-leaflet-cluster";
import L from "leaflet";
import type { Vehicle } from "@workspace/api-client-react";
import { getStatusColor } from "@/lib/utils";
import { VehicleDetails } from "./VehicleDetails";

interface BusMapProps {
  vehicles: Vehicle[];
}

// Split coordinates
const DEFAULT_CENTER: [number, number] = [43.513, 16.45];
const DEFAULT_ZOOM = 13;

const createClusterCustomIcon = function (cluster: any) {
  const count = cluster.getChildCount();
  return L.divIcon({
    html: `<span>${count}</span>`,
    className: "custom-cluster",
    iconSize: L.point(40, 40, true),
  });
};

export function BusMap({ vehicles }: BusMapProps) {
  const [selectedVehicleId, setSelectedVehicleId] = useState<number | null>(null);

  const selectedVehicle = useMemo(
    () => vehicles.find(v => v.id === selectedVehicleId) || null,
    [vehicles, selectedVehicleId]
  );

  const markers = useMemo(() => {
    return vehicles.map(vehicle => {
      const isSelected = vehicle.id === selectedVehicleId;
      const color = getStatusColor(vehicle.vehicleStatus);
      
      const html = `
        <div class="relative flex items-center justify-center w-8 h-8 transition-all duration-300 ${isSelected ? 'scale-125 z-[1000]' : 'hover:scale-110'}" style="z-index: ${isSelected ? 1000 : 1}">
          <div class="absolute inset-0 rounded-full shadow-lg border-2 ${isSelected ? 'border-white ring-4 ring-white/30' : 'border-white/80'}" style="background-color: ${color}"></div>
          <span class="relative text-white font-bold text-[11px] font-display z-10 drop-shadow-md">${vehicle.name}</span>
          ${isSelected ? `<div class="absolute -inset-2 rounded-full border border-[${color}] animate-ping opacity-50"></div>` : ''}
        </div>
      `;

      const icon = L.divIcon({
        html,
        className: 'bg-transparent',
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      return (
        <Marker
          key={vehicle.id}
          position={[vehicle.latitude, vehicle.longitude]}
          icon={icon}
          eventHandlers={{
            click: () => {
              setSelectedVehicleId(vehicle.id);
            },
          }}
          zIndexOffset={isSelected ? 1000 : 0}
        />
      );
    });
  }, [vehicles, selectedVehicleId]);

  return (
    <div className="relative w-full h-screen bg-black overflow-hidden z-0">
      <MapContainer
        center={DEFAULT_CENTER}
        zoom={DEFAULT_ZOOM}
        zoomControl={false}
        className="w-full h-full z-0"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          maxZoom={19}
        />
        
        <MarkerClusterGroup
          chunkedLoading
          iconCreateFunction={createClusterCustomIcon}
          showCoverageOnHover={false}
          maxClusterRadius={50}
        >
          {markers}
        </MarkerClusterGroup>
      </MapContainer>

      <VehicleDetails 
        vehicle={selectedVehicle} 
        onClose={() => setSelectedVehicleId(null)} 
      />
    </div>
  );
}
