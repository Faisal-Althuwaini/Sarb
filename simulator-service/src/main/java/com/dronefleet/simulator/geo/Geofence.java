package com.dronefleet.simulator.geo;

/** Permitted-operating-area check (brief Section 13, decision 8: circle first, polygon-ready). */
public interface Geofence {

	boolean contains(double lat, double lon);
}
