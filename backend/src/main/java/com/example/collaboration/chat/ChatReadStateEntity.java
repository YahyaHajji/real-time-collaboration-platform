package com.example.collaboration.chat;

import com.example.collaboration.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_read_state")
public class ChatReadStateEntity {
    @EmbeddedId
    private ChatReadStateId id;
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @MapsId("channelId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id")
    private ChatChannelEntity channel;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ChatMessageEntity lastReadMessage;
    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    protected ChatReadStateEntity() {
    }

    public ChatReadStateEntity(UserEntity user, ChatChannelEntity channel) {
        this.id = new ChatReadStateId(user.getId(), channel.getId());
        this.user = user;
        this.channel = channel;
        this.readAt = Instant.EPOCH;
    }

    public void markRead(ChatMessageEntity message) {
        this.lastReadMessage = message;
        this.readAt = message == null ? Instant.now() : message.getSentAt();
    }

    public Instant getReadAt() { return readAt; }
    public ChatMessageEntity getLastReadMessage() { return lastReadMessage; }
}

