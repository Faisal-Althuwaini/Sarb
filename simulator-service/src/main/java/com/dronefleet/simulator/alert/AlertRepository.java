package com.dronefleet.simulator.alert;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

	Optional<AlertEntity> findFirstByDroneIdAndTypeAndResolvedAtIsNull(String droneId, AlertType type);

	List<AlertEntity> findByResolvedAtIsNullOrderByTriggeredAtDesc();
}
