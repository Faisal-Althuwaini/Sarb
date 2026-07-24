package com.dronefleet.auth.security;

import com.dronefleet.auth.model.Role;

public interface JwtService {

	String generateToken(String username, Role role);
}
