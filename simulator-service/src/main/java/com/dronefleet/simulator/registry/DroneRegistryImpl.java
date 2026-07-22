package com.dronefleet.simulator.registry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.dronefleet.simulator.model.Drone;

@Component
public class DroneRegistryImpl implements DroneRegistry {

	private final Map<String, Drone> drones = new ConcurrentHashMap<>();

	@Override
	public void register(Drone drone) {
		drones.put(drone.getDroneId(), drone);
	}

	@Override
	public Collection<Drone> all() {
		return drones.values();
	}

	@Override
	public Optional<Drone> get(String droneId) {
		return Optional.ofNullable(drones.get(droneId));
	}

	@Override
	public int size() {
		return drones.size();
	}
}
