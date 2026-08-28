package com.example.collaboration.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ChatReadStateId implements Serializable {
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "channel_id")
    private UUID channelId;

    protected ChatReadStateId() {
    }

    public ChatReadStateId(Long userId, UUID channelId) {
        this.userId = userId;
        this.channelId = channelId;
    }

    public Long getUserId() { return userId; }
    public UUID getChannelId() { return channelId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChatReadStateId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, channelId); }
}

