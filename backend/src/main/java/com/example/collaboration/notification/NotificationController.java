package com.example.collaboration.notification;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        return notifications.create(request);
    }

    @GetMapping
    NotificationPageResponse list(
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication
    ) {
        return notifications.list(after, limit, authentication);
    }

    @PatchMapping("/{notificationId}/read")
    NotificationResponse markRead(@PathVariable UUID notificationId, Authentication authentication) {
        return notifications.markRead(notificationId, authentication);
    }
}
