package com.example.collaboration.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        Map<String, Object> payload,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(), notification.getType(), notification.getPayload(),
                notification.getCreatedAt(), notification.getReadAt()
        );
    }
}

