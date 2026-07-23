package com.dronefleet.simulator.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.dronefleet.simulator.model.Drone;

/** One persisted telemetry sample, written every tick for flight-log/replay (brief Section 11). */
@Entity
@Table(name = "flight_logs")
@Getter
@Setter
@NoArgsConstructor
public class FlightLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "drone_id", nullable = false)
	private String droneId;

	@Column(name = "ts", nullable = false)
	private Instant ts;

	@Column(nullable = false)
	private double lat;

	@Column(nullable = false)
	private double lon;

	@Column(name = "altitude_m", nullable = false)
	private double altitudeM;

	@Column(name = "speed_mps", nullable = false)
	private double speedMps;

	@Column(name = "heading_deg", nullable = false)
	private double headingDeg;

	@Column(name = "battery_pct", nullable = false)
	private double batteryPct;

	@Column(nullable = false)
	private String status;

	@Column(name = "mission_id")
	private String missionId;

	public static FlightLogEntity from(Drone drone) {
		FlightLogEntity entity = new FlightLogEntity();
		entity.setDroneId(drone.getDroneId());
		entity.setTs(drone.getLastUpdate());
		entity.setLat(drone.getLat());
		entity.setLon(drone.getLon());
		entity.setAltitudeM(drone.getAltitudeM());
		entity.setSpeedMps(drone.getSpeedMps());
		entity.setHeadingDeg(drone.getHeadingDeg());
		entity.setBatteryPct(drone.getBatteryPct());
		entity.setStatus(drone.getStatus().name());
		entity.setMissionId(drone.getMissionId());
		return entity;
	}
}
