package com.dronefleet.auth.model;

public record AuthResponse(String token, String username, Role role) {
}
