package com.example.collaboration.realtime;

import com.example.collaboration.chat.ChatAccessService;
import com.example.collaboration.common.LogIdentifier;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

@Component
public class StompSecurityInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompSecurityInterceptor.class);
    private static final int SEND_LIMIT = 20;
    private static final Duration SEND_WINDOW = Duration.ofSeconds(10);
    private final ConcurrentHashMap<String, SendWindow> sendWindows = new ConcurrentHashMap<>();
    private final JwtDecoder decoder;
    private final JwtAuthenticationConverter converter;
    private final ChatAccessService chatAccess;

    public StompSecurityInterceptor(
            JwtDecoder decoder,
            JwtAuthenticationConverter converter,
            ChatAccessService chatAccess
    ) {
        this.decoder = decoder;
        this.converter = converter;
        this.chatAccess = chatAccess;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        if (command == StompCommand.CONNECT) {
            authenticate(accessor);
            return message;
        }
        if (command == StompCommand.DISCONNECT) {
            if (accessor.getSessionId() != null) {
                sendWindows.remove(accessor.getSessionId());
            }
            return message;
        }
        Principal principal = accessor.getUser();
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated STOMP session is required");
        }
        if (command == StompCommand.SEND) {
            authorizeSend(accessor, authentication);
        } else if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(accessor, authentication);
        } else if (command != StompCommand.UNSUBSCRIBE) {
            throw new AccessDeniedException("This STOMP command is forbidden");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            log.warn("STOMP connect rejected reason=missing_bearer sessionHash={}",
                    LogIdentifier.hash(accessor.getSessionId()));
            throw new AccessDeniedException("A Bearer token is required");
        }
        try {
            Jwt jwt = decoder.decode(authorization.substring(7).trim());
            Authentication authentication = converter.convert(jwt);
            if (authentication == null) {
                throw new AccessDeniedException("JWT authentication could not be created");
            }
            accessor.setUser(authentication);
            log.info("STOMP connect accepted subjectHash={} sessionHash={}",
                    LogIdentifier.hash(authentication.getName()), LogIdentifier.hash(accessor.getSessionId()));
        } catch (JwtException exception) {
            log.warn("STOMP connect rejected reason=invalid_token sessionHash={}",
                    LogIdentifier.hash(accessor.getSessionId()));
            throw new AccessDeniedException("The Bearer token is invalid", exception);
        }
    }

    private void authorizeSend(StompHeaderAccessor accessor, Authentication authentication) {
        String destination = accessor.getDestination();
        if (!"/app/chat.send".equals(destination)) {
            throw new AccessDeniedException("Sending to this destination is forbidden");
        }
        String sessionId = accessor.getSessionId();
        if (sessionId != null && !allowSend(sessionId)) {
            log.warn("STOMP send rejected reason=rate_limit subjectHash={} sessionHash={}",
                    LogIdentifier.hash(authentication.getName()), LogIdentifier.hash(sessionId));
            throw new AccessDeniedException("Message rate limit exceeded");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor, Authentication authentication) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Subscription destination is required");
        }
        if (destination.equals("/user/queue/notifications")
                || destination.equals("/user/queue/errors")
                || destination.equals("/topic/presence")) {
            return;
        }
        if (destination.startsWith("/topic/roles/")) {
            String requestedRole = destination.substring("/topic/roles/".length()).toUpperCase(Locale.ROOT);
            boolean allowed = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + requestedRole));
            if (allowed) {
                return;
            }
            throw new AccessDeniedException("Role subscription is forbidden");
        }
        if (destination.startsWith("/topic/chat/")) {
            try {
                UUID channelId = UUID.fromString(destination.substring("/topic/chat/".length()));
                if (chatAccess.canAccess(channelId, authentication)) {
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                // A malformed or unauthorized channel is deliberately reported as one safe denial.
            }
            throw new AccessDeniedException("Chat subscription is forbidden");
        }
        throw new AccessDeniedException("Subscription to this destination is forbidden");
    }

    private boolean allowSend(String sessionId) {
        Instant now = Instant.now();
        SendWindow window = sendWindows.compute(sessionId, (ignored, existing) -> {
            if (existing == null || existing.startedAt().plus(SEND_WINDOW).isBefore(now)) {
                return new SendWindow(now, 1);
            }
            return new SendWindow(existing.startedAt(), existing.count() + 1);
        });
        return window.count() <= SEND_LIMIT;
    }

    private record SendWindow(Instant startedAt, int count) {
    }
}
