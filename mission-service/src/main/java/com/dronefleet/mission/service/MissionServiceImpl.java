package com.dronefleet.mission.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.dronefleet.mission.exception.DroneBusyException;
import com.dronefleet.mission.exception.MissionNotCancellableException;
import com.dronefleet.mission.exception.MissionNotFoundException;
import com.dronefleet.mission.model.MissionDto;
import com.dronefleet.mission.model.MissionEventDto;
import com.dronefleet.mission.model.MissionEventType;
import com.dronefleet.mission.model.MissionRequest;
import com.dronefleet.mission.model.MissionStatus;
import com.dronefleet.mission.model.WaypointDto;
import com.dronefleet.mission.persistence.MissionEntity;
import com.dronefleet.mission.persistence.MissionRepository;
import com.dronefleet.mission.persistence.Waypoint;

@Slf4j
@RequiredArgsConstructor
@Service
public class MissionServiceImpl implements MissionService {

	private static final List<MissionStatus> ACTIVE_STATUSES = List.of(MissionStatus.ASSIGNED,
			MissionStatus.IN_PROGRESS);

	private final MissionRepository repository;
	private final KafkaTemplate<String, MissionEventDto> kafkaTemplate;

	@Value("${sarb.kafka.topics.missions}")
	private String missionsTopic;

	@Override
	@Transactional
	public MissionDto create(MissionRequest request) {
		repository.findFirstByDroneIdAndStatusIn(request.droneId(), ACTIVE_STATUSES)
				.ifPresent(existing -> {
					throw new DroneBusyException(request.droneId());
				});

		List<Waypoint> waypoints = request.waypoints().stream().map(Waypoint::from).toList();
		MissionEntity entity = new MissionEntity(request.droneId(), waypoints, Instant.now());
		repository.save(entity);

		publish(entity, MissionEventType.ASSIGNED, request.waypoints());
		log.info("Mission {} assigned to {} with {} waypoints", entity.getId(), entity.getDroneId(),
				waypoints.size());
		return MissionDto.from(entity);
	}

	@Override
	@Transactional
	public MissionDto cancel(Long missionId) {
		MissionEntity entity = repository.findById(missionId).orElseThrow(() -> new MissionNotFoundException(missionId));
		if (!ACTIVE_STATUSES.contains(entity.getStatus())) {
			throw new MissionNotCancellableException(missionId);
		}

		entity.setStatus(MissionStatus.CANCELLED);
		entity.setCompletedAt(Instant.now());
		repository.save(entity);

		publish(entity, MissionEventType.CANCELLED, List.of());
		log.info("Mission {} cancelled", missionId);
		return MissionDto.from(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MissionDto> listAll() {
		return repository.findAllByOrderByCreatedAtDesc().stream().map(MissionDto::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MissionDto> listActive() {
		return repository.findByStatusInOrderByCreatedAtDesc(ACTIVE_STATUSES).stream().map(MissionDto::from).toList();
	}

	@Override
	@Transactional
	public void applyProgress(MissionEventDto event) {
		repository.findById(event.missionId()).ifPresentOrElse(entity -> {
			switch (event.type()) {
				case STARTED -> {
					entity.setStatus(MissionStatus.IN_PROGRESS);
					entity.setStartedAt(event.timestamp());
					repository.save(entity);
					log.info("Mission {} started", entity.getId());
				}
				case COMPLETED -> {
					entity.setStatus(MissionStatus.COMPLETED);
					entity.setCompletedAt(event.timestamp());
					repository.save(entity);
					log.info("Mission {} completed", entity.getId());
				}
				default -> {
					// ASSIGNED/CANCELLED originate from this service; nothing to apply here.
				}
			}
		}, () -> log.warn("Progress event for unknown mission {}", event.missionId()));
	}

	private void publish(MissionEntity entity, MissionEventType type, List<WaypointDto> waypoints) {
		MissionEventDto event = new MissionEventDto(entity.getId(), entity.getDroneId(), type, waypoints,
				Instant.now());
		kafkaTemplate.send(missionsTopic, entity.getDroneId(), event);
	}
}
