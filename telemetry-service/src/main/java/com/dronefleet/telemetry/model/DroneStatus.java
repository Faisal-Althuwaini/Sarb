package com.dronefleet.telemetry.model;

/** Mirrors simulator-service's enum - the Kafka wire contract (brief Section 7). */
public enum DroneStatus {
	IN_FLIGHT,
	IDLE,
	LOW_BATTERY,
	LOST_SIGNAL,
	LANDED
}
