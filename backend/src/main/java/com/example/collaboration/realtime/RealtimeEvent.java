package com.example.collaboration.realtime;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int schemaVersion,
        T payload
) {
    public static <T> RealtimeEvent<T> of(String eventType, T payload) {
        return new RealtimeEvent<>(UUID.randomUUID(), eventType, Instant.now(), 1, payload);
    }
}

