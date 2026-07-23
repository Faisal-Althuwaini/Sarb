package com.dronefleet.telemetry.persistence;

import com.dronefleet.telemetry.model.TelemetryFrame;

/** Persists one telemetry sample for flight-log/replay (brief Section 11). */
public interface FlightLogWriter {

	void record(TelemetryFrame frame);
}
