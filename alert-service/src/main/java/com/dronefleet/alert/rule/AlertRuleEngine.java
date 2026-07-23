package com.dronefleet.alert.rule;

import com.dronefleet.alert.model.TelemetryFrame;

/**
 * Applies geofence + battery + altitude rules to a telemetry frame (brief
 * Section 5/7), opening/resolving alert episodes as conditions change.
 */
public interface AlertRuleEngine {

	void evaluate(TelemetryFrame frame);
}
