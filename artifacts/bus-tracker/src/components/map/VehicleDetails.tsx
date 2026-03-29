import { X, Clock } from "lucide-react";
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

  let formattedTime = "—";
  try {
    if (vehicle.timestamp) {
      formattedTime = format(parseISO(vehicle.timestamp), "HH:mm:ss");
    }
  } catch {
    // ignore
  }

  return (
    <div className="absolute bottom-32 left-3 z-[2000] pointer-events-none animate-slide-up">
      <div className="pointer-events-auto bg-white/90 dark:bg-gray-900/90 backdrop-blur-xl border border-black/[0.06] dark:border-white/[0.08] shadow-lg rounded-xl px-3 py-2.5 relative w-56">

        {/* Close */}
        <button
          onClick={onClose}
          className="absolute top-1.5 right-1.5 w-5 h-5 rounded-full flex items-center justify-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 transition-colors"
        >
          <X className="w-3.5 h-3.5" />
        </button>

        {/* Header: line badge + status */}
        <div className="flex items-center gap-2 mb-1.5">
          <span
            className="w-8 h-8 rounded-lg flex items-center justify-center text-white text-sm font-black shrink-0"
            style={{ backgroundColor: statusColor }}
          >
            {vehicle.name}
          </span>
          <div className="min-w-0">
            <span
              className="text-[10px] font-semibold uppercase tracking-wide"
              style={{ color: statusColor }}
            >
              {statusLabel}
            </span>
            <p className="text-[11px] text-gray-500 dark:text-gray-400 truncate">
              {vehicle.registrationNumber || `#${vehicle.id}`}
            </p>
          </div>
        </div>

        {/* Details row */}
        <div className="flex items-center gap-3 text-[10px] text-gray-500 dark:text-gray-400 border-t border-black/[0.06] dark:border-white/[0.08] pt-1.5">
          {vehicle.garageNumber && (
            <span>Garaža <strong className="text-gray-700 dark:text-gray-200">{vehicle.garageNumber}</strong></span>
          )}
          <span className="flex items-center gap-0.5">
            <Clock className="w-3 h-3" />
            <strong className="text-gray-700 dark:text-gray-200">{formattedTime}</strong>
          </span>
        </div>
      </div>
    </div>
  );
}
