package com.dronefleet.mission.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.dronefleet.mission.model.MissionDto;
import com.dronefleet.mission.model.MissionRequest;
import com.dronefleet.mission.service.MissionService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/missions")
public class MissionController {

	private final MissionService missionService;

	@GetMapping
	public List<MissionDto> list() {
		return missionService.listAll();
	}

	@GetMapping("/active")
	public List<MissionDto> active() {
		return missionService.listActive();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MissionDto create(@Valid @RequestBody MissionRequest request) {
		return missionService.create(request);
	}

	@PostMapping("/{missionId}/cancel")
	public MissionDto cancel(@PathVariable Long missionId) {
		return missionService.cancel(missionId);
	}
}
