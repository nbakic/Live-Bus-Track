import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function getStatusColor(status: number): string {
  switch (status) {
    case 1: return "#22c55e"; // Green: in motion
    case 2: return "#f97316"; // Orange: starting
    case 3: return "#a855f7"; // Purple: at stop
    case 6: return "#9ca3af"; // Gray: standing
    default: return "#ef4444"; // Red: unknown
  }
}

export function getStatusLabel(status: number): string {
  switch (status) {
    case 1: return "U vožnji";
    case 2: return "Polazi";
    case 3: return "Na stanici";
    case 6: return "Miruje";
    default: return "Nepoznato";
  }
}
