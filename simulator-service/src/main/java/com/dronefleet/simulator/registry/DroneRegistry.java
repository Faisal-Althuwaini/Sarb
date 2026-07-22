package com.dronefleet.simulator.registry;

import java.util.Collection;
import java.util.Optional;

import com.dronefleet.simulator.model.Drone;

/** In-memory fleet registry, keyed by droneId. */
public interface DroneRegistry {

	void register(Drone drone);

	Collection<Drone> all();

	Optional<Drone> get(String droneId);

	int size();
}
