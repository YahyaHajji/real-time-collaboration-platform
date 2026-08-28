package com.example.collaboration.presence;

import com.example.collaboration.user.Role;

public record PresenceUserResponse(Long userId, String displayName, Role role, boolean online) {
}

