package com.dronefleet.simulator.alert;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the frontend load current alert state on mount, before the STOMP stream catches up. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

	private final AlertRepository repository;

	@GetMapping("/active")
	public List<AlertDto> active() {
		return repository.findByResolvedAtIsNullOrderByTriggeredAtDesc().stream()
				.map(AlertDto::from)
				.toList();
	}
}
