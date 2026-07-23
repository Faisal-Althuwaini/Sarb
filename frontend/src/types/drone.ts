// Mirrors the telemetry JSON contract published by simulator-service.
// Field names stay in English (wire format); Arabic labels live in i18n.
// See docs/architecture.md for the canonical schema.

export type DroneStatus = "IN_FLIGHT" | "IDLE" | "LOW_BATTERY" | "LOST_SIGNAL" | "LANDED";

export interface Position {
  lat: number;
  lon: number;
}

export interface DroneTelemetry {
  droneId: string;
  timestamp: string; // ISO-8601
  position: Position;
  altitudeM: number;
  speedMps: number;
  headingDeg: number;
  batteryPct: number;
  missionId: string | null;
  status: DroneStatus;
}
