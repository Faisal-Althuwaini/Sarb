package com.dronefleet.alert.geo;

import org.springframework.stereotype.Component;

import com.dronefleet.alert.config.GeofenceProperties;

/** Circle geofence centered on the demo area (brief Section 13, decision 8). */
@Component
public class CircleGeofence implements Geofence {

	private final double centerLat;
	private final double centerLon;
	private final double radiusKm;

	public CircleGeofence(GeofenceProperties properties) {
		this.centerLat = properties.centerLat();
		this.centerLon = properties.centerLon();
		this.radiusKm = properties.radiusKm();
	}

	@Override
	public boolean contains(double lat, double lon) {
		return GeoMath.haversineKm(centerLat, centerLon, lat, lon) <= radiusKm;
	}
}
