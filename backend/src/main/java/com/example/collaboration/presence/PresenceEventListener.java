package com.example.collaboration.presence;

import com.example.collaboration.common.LogIdentifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class PresenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(PresenceEventListener.class);
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final PresenceService presence;

    public PresenceEventListener(PresenceService presence) {
        this.presence = presence;
    }

    @EventListener
    public void connected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() == null || accessor.getSessionId() == null) {
            return;
        }
        try {
            Long userId = Long.parseLong(accessor.getUser().getName());
            sessionUsers.put(accessor.getSessionId(), userId);
            presence.connected(userId, accessor.getSessionId());
        } catch (NumberFormatException exception) {
            log.warn("Presence ignored invalid principal sessionHash={}",
                    LogIdentifier.hash(accessor.getSessionId()));
        }
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        Long userId = sessionUsers.remove(event.getSessionId());
        if (userId != null) {
            presence.disconnected(userId, event.getSessionId());
        }
    }
}
