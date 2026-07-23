package com.dronefleet.mission.service;

import java.util.List;

import com.dronefleet.mission.model.MissionDto;
import com.dronefleet.mission.model.MissionEventDto;
import com.dronefleet.mission.model.MissionRequest;

public interface MissionService {

	MissionDto create(MissionRequest request);

	MissionDto cancel(Long missionId);

	List<MissionDto> listAll();

	List<MissionDto> listActive();

	/** Applies a STARTED/COMPLETED progress event published back by simulator-service. */
	void applyProgress(MissionEventDto event);
}
