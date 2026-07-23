package com.dronefleet.telemetry.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightLogRepository extends JpaRepository<FlightLogEntity, Long> {
}
