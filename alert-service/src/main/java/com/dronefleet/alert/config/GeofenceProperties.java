package com.dronefleet.alert.config;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Demo-area circle, bound from {@code sarb.geofence.*} - mirrors simulator-service's own area. */
@Validated
@ConfigurationProperties(prefix = "sarb.geofence")
public record GeofenceProperties(
		double centerLat,
		double centerLon,
		@Positive double radiusKm) {
}
