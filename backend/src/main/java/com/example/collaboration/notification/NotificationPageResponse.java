package com.example.collaboration.notification;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> items,
        long unreadCount,
        String nextCursor
) {
}
