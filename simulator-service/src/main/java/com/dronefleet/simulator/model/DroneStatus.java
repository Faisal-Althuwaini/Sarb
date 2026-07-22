package com.dronefleet.simulator.model;

/**
 * Drone lifecycle status. Kept in sync with the frontend's
 * {@code DroneStatus} union type (frontend/src/types/drone.ts) — the enum
 * codes stay English; the frontend maps them to Arabic labels.
 */
public enum DroneStatus {
	IN_FLIGHT,
	IDLE,
	LOW_BATTERY,
	LOST_SIGNAL,
	LANDED
}
