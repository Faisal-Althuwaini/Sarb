package com.dronefleet.mission.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dronefleet.mission.model.MissionStatus;

public interface MissionRepository extends JpaRepository<MissionEntity, Long> {

	List<MissionEntity> findAllByOrderByCreatedAtDesc();

	List<MissionEntity> findByStatusInOrderByCreatedAtDesc(List<MissionStatus> statuses);

	Optional<MissionEntity> findFirstByDroneIdAndStatusIn(String droneId, List<MissionStatus> statuses);
}
