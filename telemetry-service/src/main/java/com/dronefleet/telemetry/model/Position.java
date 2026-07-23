package com.dronefleet.telemetry.model;

/** Nested lat/lon shape used in the telemetry JSON (brief Section 7). */
public record Position(double lat, double lon) {
}
