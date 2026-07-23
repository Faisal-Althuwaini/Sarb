package com.dronefleet.simulator.alert;

import com.dronefleet.simulator.model.Drone;

/**
 * Applies geofence + battery + altitude rules to a drone's current state
 * (brief Section 5/7), opening/resolving alert episodes as conditions change.
 */
public interface AlertRuleEngine {

	void evaluate(Drone drone);
}
