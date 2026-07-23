package com.dronefleet.mission.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.dronefleet.mission.model.MissionStatus;

/** One assigned route: a drone plus an ordered list of waypoints it flies in sequence. */
@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
public class MissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "drone_id", nullable = false)
	private String droneId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MissionStatus status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "mission_waypoints", joinColumns = @JoinColumn(name = "mission_id"))
	@OrderColumn(name = "seq")
	private List<Waypoint> waypoints = new ArrayList<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	public MissionEntity(String droneId, List<Waypoint> waypoints, Instant createdAt) {
		this.droneId = droneId;
		this.waypoints = waypoints;
		this.status = MissionStatus.ASSIGNED;
		this.createdAt = createdAt;
	}
}
