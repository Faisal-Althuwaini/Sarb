package com.dronefleet.auth.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.dronefleet.auth.exception.InvalidCredentialsException;
import com.dronefleet.auth.exception.UsernameTakenException;
import com.dronefleet.auth.model.AuthResponse;
import com.dronefleet.auth.model.LoginRequest;
import com.dronefleet.auth.model.RegisterRequest;
import com.dronefleet.auth.model.Role;
import com.dronefleet.auth.persistence.UserEntity;
import com.dronefleet.auth.persistence.UserRepository;
import com.dronefleet.auth.security.JwtService;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (repository.existsByUsername(request.username())) {
			throw new UsernameTakenException(request.username());
		}

		UserEntity user = new UserEntity(request.username(), passwordEncoder.encode(request.password()),
				Role.OPERATOR, Instant.now());
		repository.save(user);
		log.info("Registered user {}", user.getUsername());

		String token = jwtService.generateToken(user.getUsername(), user.getRole());
		return new AuthResponse(token, user.getUsername(), user.getRole());
	}

	@Override
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		UserEntity user = repository.findByUsername(request.username())
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		String token = jwtService.generateToken(user.getUsername(), user.getRole());
		return new AuthResponse(token, user.getUsername(), user.getRole());
	}
}
