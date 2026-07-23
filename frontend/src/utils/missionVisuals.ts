import type { MissionStatus } from "../types/mission";

export function missionStatusBadgeVariant(status: MissionStatus): "default" | "secondary" | "outline" {
  switch (status) {
    case "IN_PROGRESS":
      return "default";
    case "ASSIGNED":
      return "secondary";
    default: // COMPLETED, CANCELLED
      return "outline";
  }
}
