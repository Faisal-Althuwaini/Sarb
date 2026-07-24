package com.dronefleet.gateway.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import lombok.RequiredArgsConstructor;

// Gatekeeper for every route except /api/auth/**: requires a valid
// "Authorization: Bearer <jwt>" header, signed by auth-service's secret.
// Rejects with 401 before the request ever reaches a backend service.
@RequiredArgsConstructor
@Component
public class AuthFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtValidator jwtValidator;

	@Override
	public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
		String header = request.headers().firstHeader("Authorization");
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return ServerResponse.status(401).build();
		}

		String token = header.substring(BEARER_PREFIX.length());
		if (jwtValidator.validate(token).isEmpty()) {
			return ServerResponse.status(401).build();
		}

		return next.handle(request);
	}
}
