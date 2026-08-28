package com.example.collaboration.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    @Query("""
            select notification from NotificationEntity notification
            where notification.recipient.id = :recipientId
              and (notification.createdAt > :afterTime
                or (notification.createdAt = :afterTime and notification.id > :afterId))
            order by notification.createdAt asc, notification.id asc
            """)
    List<NotificationEntity> findAfter(
            @Param("recipientId") Long recipientId,
            @Param("afterTime") Instant afterTime,
            @Param("afterId") UUID afterId,
            Pageable pageable
    );
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
}
