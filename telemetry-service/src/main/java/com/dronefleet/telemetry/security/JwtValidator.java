package com.dronefleet.telemetry.security;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Verifies tokens issued by auth-service's JwtServiceImpl - same secret,
// same signing scheme, mirrored here (and in gateway) rather than shared as
// a library module, same tradeoff as the DTOs mirrored across the
// Kafka-connected services.
@Component
public class JwtValidator {

	private final SecretKey key;

	public JwtValidator(@Value("${sarb.jwt.secret}") String secret) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	/** Returns the token's subject (username) if valid, empty otherwise - never throws. */
	public Optional<String> validate(String token) {
		try {
			return Optional.of(Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload()
					.getSubject());
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
