package com.example.collaboration.realtime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.collaboration.chat.ChatAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class StompSecurityInterceptorTest {

    private final StompSecurityInterceptor interceptor = new StompSecurityInterceptor(
            mock(JwtDecoder.class), mock(JwtAuthenticationConverter.class), mock(ChatAccessService.class)
    );

    @Test
    void roleSubscriptionsRequireAnExactAuthorityAndUnknownDestinationsAreDenied() {
        var admin = authentication("ROLE_ADMIN");
        var user = authentication("ROLE_USER");

        assertThatNoException().isThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/roles/ADMIN", admin, "admin-session"), mock(org.springframework.messaging.MessageChannel.class)
        ));
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/roles/ADMIN", user, "user-session"), mock(org.springframework.messaging.MessageChannel.class)
        )).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/not-allowed", admin, "admin-session"), mock(org.springframework.messaging.MessageChannel.class)
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sendRateIsBoundedPerSession() {
        var user = authentication("ROLE_USER");
        for (int index = 0; index < 20; index++) {
            interceptor.preSend(
                    frame(StompCommand.SEND, "/app/chat.send", user, "rate-session"),
                    mock(org.springframework.messaging.MessageChannel.class)
            );
        }
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SEND, "/app/chat.send", user, "rate-session"),
                mock(org.springframework.messaging.MessageChannel.class)
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("rate limit");
    }

    private TestingAuthenticationToken authentication(String role) {
        return new TestingAuthenticationToken("3", null, role);
    }

    private Message<byte[]> frame(
            StompCommand command,
            String destination,
            TestingAuthenticationToken authentication,
            String sessionId
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setSessionId(sessionId);
        accessor.setUser(authentication);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
