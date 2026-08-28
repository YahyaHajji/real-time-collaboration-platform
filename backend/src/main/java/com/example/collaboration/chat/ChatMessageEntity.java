package com.example.collaboration.chat;

import com.example.collaboration.common.UuidV7;
import com.example.collaboration.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {
    @Id
    private UUID id;
    @Column(name = "client_message_id", nullable = false)
    private UUID clientMessageId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChatChannelEntity channel;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private UserEntity sender;
    @Column(nullable = false, length = 2000)
    private String body;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected ChatMessageEntity() {
    }

    public ChatMessageEntity(UUID clientMessageId, ChatChannelEntity channel, UserEntity sender, String body) {
        this.id = UuidV7.generate();
        this.clientMessageId = clientMessageId;
        this.channel = channel;
        this.sender = sender;
        this.body = body;
        this.sentAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getClientMessageId() { return clientMessageId; }
    public ChatChannelEntity getChannel() { return channel; }
    public UserEntity getSender() { return sender; }
    public String getBody() { return body; }
    public Instant getSentAt() { return sentAt; }
}

