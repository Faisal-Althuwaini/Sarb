package com.dronefleet.simulator.geo;

import org.springframework.stereotype.Component;

import com.dronefleet.simulator.config.SimulatorProperties;

/** Circle geofence centered on the demo area, reusing the simulator's own spread radius. */
@Component
public class CircleGeofence implements Geofence {

	private final double centerLat;
	private final double centerLon;
	private final double radiusKm;

	public CircleGeofence(SimulatorProperties properties) {
		this.centerLat = properties.centerLat();
		this.centerLon = properties.centerLon();
		this.radiusKm = properties.spreadRadiusKm();
	}

	@Override
	public boolean contains(double lat, double lon) {
		return GeoMath.haversineKm(centerLat, centerLon, lat, lon) <= radiusKm;
	}
}
