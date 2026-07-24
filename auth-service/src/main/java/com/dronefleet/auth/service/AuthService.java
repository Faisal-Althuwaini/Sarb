package com.dronefleet.auth.service;

import com.dronefleet.auth.model.AuthResponse;
import com.dronefleet.auth.model.LoginRequest;
import com.dronefleet.auth.model.RegisterRequest;

public interface AuthService {

	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);
}
