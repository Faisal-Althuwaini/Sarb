import type { DroneTelemetry } from "../types/drone";

// Color thresholds by battery percentage; LOST_SIGNAL always renders grey
// regardless of battery, since a stale/no-signal drone is the more urgent fact.
export function droneColor(drone: DroneTelemetry): string {
  if (drone.status === "LOST_SIGNAL") return "#9ca3af"; // grey
  if (drone.batteryPct <= 20) return "#ef4444"; // red
  if (drone.batteryPct <= 50) return "#f59e0b"; // amber
  return "#22c55e"; // green
}

// shadcn Badge variant to pair with a drone's status in the detail popup.
export function droneStatusBadgeVariant(
  drone: DroneTelemetry,
): "default" | "secondary" | "destructive" | "outline" {
  if (drone.status === "LOST_SIGNAL") return "destructive";
  if (drone.status === "LOW_BATTERY") return "destructive";
  if (drone.status === "IDLE") return "secondary";
  if (drone.status === "LANDED") return "outline";
  return "default";
}
