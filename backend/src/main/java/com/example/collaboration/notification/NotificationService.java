package com.example.collaboration.notification;

import com.example.collaboration.auth.CurrentUserService;
import com.example.collaboration.common.ForbiddenOperationException;
import com.example.collaboration.common.ResourceNotFoundException;
import com.example.collaboration.common.TimeUuidCursor;
import com.example.collaboration.realtime.RealtimePublisher;
import com.example.collaboration.user.UserEntity;
import com.example.collaboration.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final CurrentUserService currentUser;
    private final RealtimePublisher realtime;

    public NotificationService(
            NotificationRepository notifications,
            UserRepository users,
            CurrentUserService currentUser,
            RealtimePublisher realtime
    ) {
        this.notifications = notifications;
        this.users = users;
        this.currentUser = currentUser;
        this.realtime = realtime;
    }

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        UserEntity recipient = users.findById(request.recipientUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification recipient was not found"));
        NotificationEntity saved = notifications.save(new NotificationEntity(
                recipient, request.type(), Map.of("message", request.message().trim())
        ));
        NotificationResponse response = NotificationResponse.from(saved);
        realtime.afterCommitToUser(recipient.getId(), "/queue/notifications", "notification.created", response);
        return response;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse list(String after, int limit, Authentication authentication) {
        Long userId = currentUser.requireUserId(authentication);
        TimeUuidCursor cursor = after == null ? TimeUuidCursor.afterStart() : TimeUuidCursor.parse(after);
        List<NotificationResponse> items = notifications
                .findAfter(
                        userId, cursor.timestamp(), cursor.id(), PageRequest.of(0, Math.clamp(limit, 1, 100))
                )
                .stream().map(NotificationResponse::from).toList();
        TimeUuidCursor next = items.isEmpty()
                ? cursor
                : new TimeUuidCursor(items.getLast().createdAt(), items.getLast().id());
        return new NotificationPageResponse(
                items, notifications.countByRecipientIdAndReadAtIsNull(userId), next.encode()
        );
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId, Authentication authentication) {
        Long userId = currentUser.requireUserId(authentication);
        NotificationEntity notification = notifications.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found"));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ForbiddenOperationException("You cannot update this notification");
        }
        notification.markRead();
        return NotificationResponse.from(notification);
    }
}
