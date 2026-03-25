import { X, MapPin, Hash, Clock, Bus } from "lucide-react";
import { format, parseISO } from "date-fns";
import type { Vehicle } from "@workspace/api-client-react";
import { getStatusColor, getStatusLabel } from "@/lib/utils";

interface VehicleDetailsProps {
  vehicle: Vehicle | null;
  onClose: () => void;
}

export function VehicleDetails({ vehicle, onClose }: VehicleDetailsProps) {
  if (!vehicle) return null;

  const statusColor = getStatusColor(vehicle.vehicleStatus);
  const statusLabel = getStatusLabel(vehicle.vehicleStatus);
  
  let formattedTime = "Nepoznato";
  try {
    if (vehicle.timestamp) {
      formattedTime = format(parseISO(vehicle.timestamp), "dd.MM.yyyy HH:mm:ss");
    }
  } catch (e) {
    // ignore parse error
  }

  return (
    <div className="absolute bottom-24 left-4 right-4 z-[2000] pointer-events-none flex justify-center animate-slide-up">
      <div className="bg-background/95 backdrop-blur-2xl border border-white/10 shadow-2xl rounded-3xl p-6 pointer-events-auto w-full max-w-md relative overflow-hidden">
        
        {/* Color accent line at top */}
        <div 
          className="absolute top-0 left-0 right-0 h-1.5 w-full"
          style={{ backgroundColor: statusColor }}
        />

        <button 
          onClick={onClose}
          className="absolute top-4 right-4 p-2 bg-white/5 hover:bg-white/10 rounded-full text-muted-foreground hover:text-foreground transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex gap-6 items-start">
          {/* Big Line Badge */}
          <div 
            className="flex-shrink-0 w-20 h-20 rounded-2xl flex items-center justify-center shadow-lg border border-white/20"
            style={{ backgroundColor: statusColor }}
          >
            <span className="text-3xl font-display font-black text-white drop-shadow-md">
              {vehicle.name}
            </span>
          </div>

          <div className="flex-1 pt-1">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-bold tracking-wider uppercase text-muted-foreground">Status</span>
              <span 
                className="px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider border"
                style={{ color: statusColor, borderColor: `${statusColor}40`, backgroundColor: `${statusColor}10` }}
              >
                {statusLabel}
              </span>
            </div>
            
            <h3 className="text-xl font-bold text-foreground mb-4">
              Vozilo #{vehicle.id}
            </h3>

            <div className="grid grid-cols-2 gap-y-4 gap-x-2">
              <div className="flex items-start gap-2 text-sm text-muted-foreground">
                <Hash className="w-4 h-4 mt-0.5 text-primary" />
                <div>
                  <p className="font-medium text-foreground/70">Registracija</p>
                  <p className="font-mono text-foreground">{vehicle.registrationNumber || "N/A"}</p>
                </div>
              </div>
              
              <div className="flex items-start gap-2 text-sm text-muted-foreground">
                <Bus className="w-4 h-4 mt-0.5 text-primary" />
                <div>
                  <p className="font-medium text-foreground/70">Garažni Br.</p>
                  <p className="font-mono text-foreground">{vehicle.garageNumber || "N/A"}</p>
                </div>
              </div>

              <div className="flex items-start gap-2 text-sm text-muted-foreground">
                <MapPin className="w-4 h-4 mt-0.5 text-primary" />
                <div>
                  <p className="font-medium text-foreground/70">Lokacija</p>
                  <p className="font-mono text-xs text-foreground mt-0.5">
                    {vehicle.latitude.toFixed(4)}, {vehicle.longitude.toFixed(4)}
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-2 text-sm text-muted-foreground">
                <Clock className="w-4 h-4 mt-0.5 text-primary" />
                <div>
                  <p className="font-medium text-foreground/70">Zadnji Signal</p>
                  <p className="font-mono text-xs text-foreground mt-0.5">{formattedTime}</p>
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}
