package com.dronefleet.mission.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.dronefleet.mission.model.WaypointDto;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Waypoint {

	@Column(nullable = false)
	private double lat;

	@Column(nullable = false)
	private double lon;

	@Column(name = "altitude_m", nullable = false)
	private double altitudeM;

	public Waypoint(double lat, double lon, double altitudeM) {
		this.lat = lat;
		this.lon = lon;
		this.altitudeM = altitudeM;
	}

	public static Waypoint from(WaypointDto dto) {
		return new Waypoint(dto.lat(), dto.lon(), dto.altitudeM());
	}

	public WaypointDto toDto() {
		return new WaypointDto(lat, lon, altitudeM);
	}
}
