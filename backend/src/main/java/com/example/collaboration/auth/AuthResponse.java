package com.example.collaboration.auth;

import com.example.collaboration.user.Role;
import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        Long id,
        String email,
        String displayName,
        Role role
) {
}

