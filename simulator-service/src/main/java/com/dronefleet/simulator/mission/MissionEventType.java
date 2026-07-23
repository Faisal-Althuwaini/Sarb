package com.dronefleet.simulator.mission;

/**
 * Mirrors mission-service's enum of the same name (Phase 4 - each service
 * keeps its own copy of shape classes rather than sharing a library module).
 */
public enum MissionEventType {
	ASSIGNED,
	STARTED,
	COMPLETED,
	CANCELLED
}
