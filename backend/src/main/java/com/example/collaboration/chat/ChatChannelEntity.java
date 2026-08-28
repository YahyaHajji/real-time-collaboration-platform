package com.example.collaboration.chat;

import com.example.collaboration.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_channel")
public class ChatChannelEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", length = 20)
    private Role requiredRole;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChatChannelEntity() {
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Role getRequiredRole() { return requiredRole; }
    public Instant getCreatedAt() { return createdAt; }
}

