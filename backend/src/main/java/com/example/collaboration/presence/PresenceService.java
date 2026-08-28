package com.example.collaboration.presence;

import com.example.collaboration.common.ResourceNotFoundException;
import com.example.collaboration.common.LogIdentifier;
import com.example.collaboration.realtime.RealtimePublisher;
import com.example.collaboration.user.UserEntity;
import com.example.collaboration.user.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
    private final ConcurrentHashMap<Long, Set<String>> sessions = new ConcurrentHashMap<>();
    private final UserRepository users;
    private final RealtimePublisher realtime;

    public PresenceService(UserRepository users, RealtimePublisher realtime) {
        this.users = users;
        this.realtime = realtime;
    }

    public void connected(Long userId, String sessionId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connected user was not found"));
        Set<String> userSessions = sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = userSessions.isEmpty();
        userSessions.add(sessionId);
        if (wasOffline) {
            PresenceUserResponse event = response(user, true);
            realtime.nowToDestination("/topic/presence", "presence.online", event);
            log.info("Presence changed outcome=online subjectHash={} sessions={}",
                    LogIdentifier.hash(userId), userSessions.size());
        }
    }

    public void disconnected(Long userId, String sessionId) {
        Set<String> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(sessionId);
        if (userSessions.isEmpty() && sessions.remove(userId, userSessions)) {
            users.findById(userId).ifPresent(user -> realtime.nowToDestination(
                    "/topic/presence", "presence.offline", response(user, false)
            ));
            log.info("Presence changed outcome=offline subjectHash={}", LogIdentifier.hash(userId));
        }
    }

    public List<PresenceUserResponse> snapshot() {
        return users.findAllById(sessions.keySet()).stream()
                .map(user -> response(user, true))
                .sorted(java.util.Comparator.comparing(PresenceUserResponse::displayName))
                .toList();
    }

    private PresenceUserResponse response(UserEntity user, boolean online) {
        return new PresenceUserResponse(user.getId(), user.getDisplayName(), user.getRole(), online);
    }
}
