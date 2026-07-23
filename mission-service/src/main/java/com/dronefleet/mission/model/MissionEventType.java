package com.dronefleet.mission.model;

/**
 * mission.events lifecycle. ASSIGNED/CANCELLED are published by mission-service
 * (the system of record); STARTED/COMPLETED are published back by simulator-service,
 * the only party that knows when a drone actually begins/finishes flying the route.
 */
public enum MissionEventType {
	ASSIGNED,
	STARTED,
	COMPLETED,
	CANCELLED
}
