package com.dronefleet.simulator.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightLogRepository extends JpaRepository<FlightLogEntity, Long> {
}
