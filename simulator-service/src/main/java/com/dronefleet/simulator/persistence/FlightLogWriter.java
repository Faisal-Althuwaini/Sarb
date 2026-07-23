package com.dronefleet.simulator.persistence;

import java.util.Collection;

import com.dronefleet.simulator.model.Drone;

/** Persists the fleet's state once per tick for flight-log/replay (brief Section 11). */
public interface FlightLogWriter {

	void record(Collection<Drone> drones);
}
