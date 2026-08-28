package com.example.collaboration.realtime;

import java.time.Instant;

public record RealtimeCommandError(String code, String message, Instant occurredAt) {
    public static RealtimeCommandError safe(String message) {
        return new RealtimeCommandError("COMMAND_REJECTED", message, Instant.now());
    }
}

