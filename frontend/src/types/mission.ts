// Mirrors the mission JSON contract published by mission-service on
// /topic/missions (relayed by telemetry-service) and served by its REST API.
// Field names stay English (wire format); Arabic labels live in i18n.

export type MissionStatus = "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type MissionEventType = "ASSIGNED" | "STARTED" | "COMPLETED" | "CANCELLED";

export interface Waypoint {
  lat: number;
  lon: number;
  altitudeM: number;
}

export interface Mission {
  missionId: number;
  droneId: string;
  status: MissionStatus;
  waypoints: Waypoint[];
  createdAt: string; // ISO-8601
  startedAt: string | null;
  completedAt: string | null;
}

// The live /topic/missions payload - a lifecycle event, not the full mission
// record. waypoints is only populated on ASSIGNED.
export interface MissionEvent {
  missionId: number;
  droneId: string;
  type: MissionEventType;
  waypoints: Waypoint[];
  timestamp: string; // ISO-8601
}
