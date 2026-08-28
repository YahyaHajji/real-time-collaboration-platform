package com.example.collaboration.chat;

import com.example.collaboration.common.ForbiddenOperationException;
import com.example.collaboration.common.ResourceNotFoundException;
import com.example.collaboration.user.Role;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatAccessService {

    private final ChatChannelRepository channels;

    public ChatAccessService(ChatChannelRepository channels) {
        this.channels = channels;
    }

    @Transactional(readOnly = true)
    public ChatChannelEntity requireAccessible(UUID channelId, Authentication authentication) {
        ChatChannelEntity channel = channels.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat channel was not found"));
        if (!canAccess(channel, authentication)) {
            throw new ForbiddenOperationException("You cannot access this chat channel");
        }
        return channel;
    }

    @Transactional(readOnly = true)
    public boolean canAccess(UUID channelId, Authentication authentication) {
        return channels.findById(channelId).map(channel -> canAccess(channel, authentication)).orElse(false);
    }

    public boolean canAccess(ChatChannelEntity channel, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Role required = channel.getRequiredRole();
        return required == null || authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + required.name()));
    }
}

