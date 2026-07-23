package com.dronefleet.mission.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DroneBusyException extends RuntimeException {

	public DroneBusyException(String droneId) {
		super("Drone " + droneId + " already has an active mission");
	}
}
