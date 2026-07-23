package com.dronefleet.alert.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dronefleet.alert.model.AlertType;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

	Optional<AlertEntity> findFirstByDroneIdAndTypeAndResolvedAtIsNull(String droneId, AlertType type);

	List<AlertEntity> findByResolvedAtIsNullOrderByTriggeredAtDesc();
}
