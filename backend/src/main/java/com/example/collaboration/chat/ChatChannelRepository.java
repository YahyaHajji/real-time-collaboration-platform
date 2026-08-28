package com.example.collaboration.chat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatChannelRepository extends JpaRepository<ChatChannelEntity, UUID> {
    List<ChatChannelEntity> findAllByOrderByNameAsc();
}

