package com.example.collaboration.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.collaboration.realtime.RealtimePublisher;
import com.example.collaboration.user.Role;
import com.example.collaboration.user.UserEntity;
import com.example.collaboration.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PresenceServiceTest {

    @Test
    void multipleSessionsProduceOnlyFirstOnlineAndFinalOfflineTransitions() {
        UserRepository users = mock(UserRepository.class);
        RealtimePublisher realtime = mock(RealtimePublisher.class);
        UserEntity user = new UserEntity("user@example.test", "hash", "Test User", Role.USER);
        setId(user, 7L);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.findAllById(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Iterable<?> ids = invocation.getArgument(0);
            return ids.iterator().hasNext() ? List.of(user) : List.of();
        });
        PresenceService presence = new PresenceService(users, realtime);

        presence.connected(7L, "one");
        presence.connected(7L, "two");
        assertThat(presence.snapshot()).hasSize(1);
        presence.disconnected(7L, "one");
        assertThat(presence.snapshot()).hasSize(1);
        presence.disconnected(7L, "two");
        assertThat(presence.snapshot()).isEmpty();

        verify(realtime, times(2)).nowToDestination(
                org.mockito.ArgumentMatchers.eq("/topic/presence"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private void setId(UserEntity user, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
