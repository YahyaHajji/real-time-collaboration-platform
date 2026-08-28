package com.example.collaboration.chat;

import com.example.collaboration.user.Role;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID clientMessageId,
        UUID channelId,
        Long senderId,
        String senderName,
        Role senderRole,
        String body,
        Instant sentAt
) {
    public static ChatMessageResponse from(ChatMessageEntity message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getClientMessageId(),
                message.getChannel().getId(),
                message.getSender().getId(),
                message.getSender().getDisplayName(),
                message.getSender().getRole(),
                message.getBody(),
                message.getSentAt()
        );
    }
}

