package com.example.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("development")
class CollaborationIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.4-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @LocalServerPort int port;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    @AfterEach
    void closeSocket() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void jwtSecuresRestAndStompWhileChatPersistsAndBroadcasts() throws Exception {
        String userToken = login("user@collaboration.local", "User123!");

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", bearer(userToken));
        stompSession = stompClient.connectAsync(
                URI.create("ws://localhost:" + port + "/ws"),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() { }
        ).get(10, TimeUnit.SECONDS);

        MvcResult channelsResult = mockMvc.perform(get("/api/channels")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode channels = objectMapper.readTree(channelsResult.getResponse().getContentAsString());
        String generalId = channels.get(0).get("id").asText();
        ArrayBlockingQueue<Map<?, ?>> received = new ArrayBlockingQueue<>(1);
        stompSession.subscribe("/topic/chat/" + generalId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    received.offer(objectMapper.readValue((byte[]) payload, Map.class));
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }
        });
        UUID clientMessageId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of(
                "channelId", generalId,
                "clientMessageId", clientMessageId.toString(),
                "body", "Secured hello"
        ));
        StompHeaders sendHeaders = new StompHeaders();
        sendHeaders.setDestination("/app/chat.send");
        sendHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        stompSession.send(sendHeaders, body.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        Map<?, ?> event = received.poll(10, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.get("eventType")).isEqualTo("chat.message.created");
        stompSession.send(sendHeaders, body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mockMvc.perform(get("/api/channels/{id}/messages", generalId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("Secured hello"));

        mockMvc.perform(get("/api/channels/{id}/messages", "00000000-0000-7000-8000-000000000002")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCredentialsReturnTheSameGenericUnauthorizedContract() throws Exception {
        assertInvalidCredentials("user@collaboration.local", "wrong-password");
        assertInvalidCredentials("missing@collaboration.local", "wrong-password");
    }

    @Test
    void invalidStompTokenCannotEstablishSession() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer invalid");
        var future = stompClient.connectAsync(
                URI.create("ws://localhost:" + port + "/ws"),
                new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() { }
        );
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
    }

    @Test
    void missingAndExpiredStompTokensCannotEstablishSessions() {
        assertConnectRejected(new StompHeaders());
        StompHeaders expired = new StompHeaders();
        expired.add("Authorization", bearer(token(
                "real-time-collaboration-api", "real-time-collaboration-web",
                Instant.now().minusSeconds(300), Instant.now().minusSeconds(120), "3", "USER"
        )));
        assertConnectRejected(expired);
    }

    @Test
    void decoderRejectsWrongIssuerAudienceSubjectAndRole() {
        Instant now = Instant.now();
        assertRejected(token("wrong-issuer", "real-time-collaboration-web", now, now.plusSeconds(60), "3", "USER"));
        assertRejected(token("real-time-collaboration-api", "wrong-audience", now, now.plusSeconds(60), "3", "USER"));
        assertRejected(token("real-time-collaboration-api", "real-time-collaboration-web", now, now.plusSeconds(60), "not-a-number", "USER"));
        assertRejected(token("real-time-collaboration-api", "real-time-collaboration-web", now, now.plusSeconds(60), "3", "OWNER"));
    }

    @Test
    void managerCanCreateOfflineNotificationAndRecipientRecoversIt() throws Exception {
        String managerToken = login("manager@collaboration.local", "Manager123!");
        String userToken = login("user@collaboration.local", "User123!");
        JsonNode user = objectMapper.readTree(mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(userToken)))
                .andReturn().getResponse().getContentAsString());

        MvcResult created = mockMvc.perform(post("/api/notifications")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientUserId", user.get("id").asLong(),
                                "type", "task.assigned",
                                "message", "A durable offline notification"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String notificationId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].payload.message").value("A durable offline notification"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/notifications/{id}/read", notificationId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/notifications/{id}/read", notificationId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isNotEmpty());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private void assertInvalidCredentials(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("The email or password is incorrect"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertConnectRejected(StompHeaders headers) {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        try {
            var future = client.connectAsync(
                    URI.create("ws://localhost:" + port + "/ws"),
                    new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() { }
            );
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(Exception.class);
        } finally {
            client.stop();
        }
    }

    private void assertRejected(String token) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    private String token(
            String issuer, String audience, Instant issuedAt, Instant expiresAt, String subject, String role
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(subject)
                .claim("role", role)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims
        )).getTokenValue();
    }
}
