package com.dronefleet.simulator.registry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.dronefleet.simulator.model.Drone;

/** In-memory fleet registry, keyed by droneId. */
@Component
public class DroneRegistry {

	private final Map<String, Drone> drones = new ConcurrentHashMap<>();

	public void register(Drone drone) {
		drones.put(drone.getDroneId(), drone);
	}

	public Collection<Drone> all() {
		return drones.values();
	}

	public Drone get(String droneId) {
		return drones.get(droneId);
	}

	public int size() {
		return drones.size();
	}
}
