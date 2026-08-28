package com.example.collaboration.chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReadStateRepository extends JpaRepository<ChatReadStateEntity, ChatReadStateId> {
}

