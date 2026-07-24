import type { DroneTelemetry } from "../types/drone";

// Tactical HUD palette: phosphor green (nominal) / amber (caution) / red (critical).
// LOST_SIGNAL always renders red regardless of battery, since a stale/no-signal
// drone is the more urgent fact.
export function droneColor(drone: DroneTelemetry): string {
  if (drone.status === "LOST_SIGNAL") return "#ff3b3b";
  if (drone.batteryPct <= 20) return "#ff3b3b";
  if (drone.batteryPct <= 50) return "#ffb300";
  return "#39ff88";
}

// Whether this drone's marker should pulse for attention on the map.
export function droneIsCritical(drone: DroneTelemetry): boolean {
  return drone.status === "LOST_SIGNAL" || drone.batteryPct <= 20;
}

// shadcn Badge variant to pair with a drone's status in the detail popup.
export function droneStatusBadgeVariant(
  drone: DroneTelemetry,
): "default" | "secondary" | "caution" | "destructive" | "outline" {
  if (drone.status === "LOST_SIGNAL") return "destructive";
  if (drone.status === "LOW_BATTERY") return "caution";
  if (drone.status === "IDLE") return "secondary";
  if (drone.status === "LANDED") return "outline";
  return "default";
}
