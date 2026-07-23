package com.dronefleet.mission.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MissionNotCancellableException extends RuntimeException {

	public MissionNotCancellableException(Long missionId) {
		super("Mission " + missionId + " is already finished and can't be cancelled");
	}
}
