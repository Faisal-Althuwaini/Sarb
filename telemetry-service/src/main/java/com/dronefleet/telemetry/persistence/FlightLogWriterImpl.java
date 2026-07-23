package com.dronefleet.telemetry.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dronefleet.telemetry.model.TelemetryFrame;

@RequiredArgsConstructor
@Component
public class FlightLogWriterImpl implements FlightLogWriter {

	private final FlightLogRepository repository;

	@Override
	@Transactional
	public void record(TelemetryFrame frame) {
		repository.save(FlightLogEntity.from(frame));
	}
}
