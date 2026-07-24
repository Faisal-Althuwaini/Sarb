package com.dronefleet.auth.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.dronefleet.auth.model.Role;

@Service
public class JwtServiceImpl implements JwtService {

	private final SecretKey key;
	private final long expirationMinutes;

	public JwtServiceImpl(@Value("${sarb.jwt.secret}") String secret,
			@Value("${sarb.jwt.expiration-minutes}") long expirationMinutes) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		this.expirationMinutes = expirationMinutes;
	}

	@Override
	public String generateToken(String username, Role role) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(username)
				.claim("role", role.name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
				.signWith(key)
				.compact();
	}
}
