package com.example.collaboration.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    Optional<ChatMessageEntity> findBySenderIdAndClientMessageId(Long senderId, UUID clientMessageId);
    @Query("""
            select message from ChatMessageEntity message
            where message.channel.id = :channelId
              and (message.sentAt < :beforeTime
                or (message.sentAt = :beforeTime and message.id < :beforeId))
            order by message.sentAt desc, message.id desc
            """)
    List<ChatMessageEntity> findBefore(
            @Param("channelId") UUID channelId,
            @Param("beforeTime") Instant beforeTime,
            @Param("beforeId") UUID beforeId,
            Pageable pageable
    );
    long countByChannelId(UUID channelId);
    long countByChannelIdAndSentAtAfter(UUID channelId, Instant sentAt);
}
