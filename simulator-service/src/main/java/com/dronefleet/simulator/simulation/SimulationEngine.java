package com.dronefleet.simulator.simulation;

/**
 * Seeds the virtual fleet around the demo center and advances it on each
 * simulation tick, broadcasting telemetry (brief Section 8).
 */
public interface SimulationEngine {

	void seedFleet();

	void tick();
}
