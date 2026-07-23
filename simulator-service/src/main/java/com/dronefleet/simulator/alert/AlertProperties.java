package com.dronefleet.simulator.alert;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sarb.alert")
public record AlertProperties(
		@Positive double lowBatteryPct,
		@Positive double criticalBatteryPct,
		@Positive double maxAltitudeM) {
}
