package com.example.collaboration.chat;

import com.example.collaboration.user.Role;
import java.util.UUID;

public record ChannelResponse(UUID id, String name, Role requiredRole, long unreadCount) {
}

