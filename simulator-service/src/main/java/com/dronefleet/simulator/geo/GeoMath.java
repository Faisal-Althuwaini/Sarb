package com.dronefleet.simulator.geo;

/** Shared great-circle math used by the simulation engine and geofence checks. */
public final class GeoMath {

	private static final double EARTH_RADIUS_KM = 6371.0;
	private static final double METERS_PER_DEGREE_LAT = 111_320.0;

	private GeoMath() {
	}

	public static double metersPerDegreeLat() {
		return METERS_PER_DEGREE_LAT;
	}

	public static double normalizeDegrees(double deg) {
		double d = deg % 360.0;
		return d < 0 ? d + 360.0 : d;
	}

	public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
						* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_KM * c;
	}

	public static double bearingDegrees(double fromLat, double fromLon, double toLat, double toLon) {
		double lat1 = Math.toRadians(fromLat);
		double lat2 = Math.toRadians(toLat);
		double dLon = Math.toRadians(toLon - fromLon);
		double y = Math.sin(dLon) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
		return normalizeDegrees(Math.toDegrees(Math.atan2(y, x)));
	}
}
