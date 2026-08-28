package com.example.collaboration.chat;

import com.example.collaboration.auth.CurrentUserService;
import com.example.collaboration.common.ForbiddenOperationException;
import com.example.collaboration.common.ResourceNotFoundException;
import com.example.collaboration.common.TimeUuidCursor;
import com.example.collaboration.realtime.RealtimePublisher;
import com.example.collaboration.user.UserEntity;
import com.example.collaboration.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatChannelRepository channels;
    private final ChatMessageRepository messages;
    private final ChatReadStateRepository readStates;
    private final UserRepository users;
    private final ChatAccessService access;
    private final CurrentUserService currentUser;
    private final RealtimePublisher realtime;

    public ChatService(
            ChatChannelRepository channels,
            ChatMessageRepository messages,
            ChatReadStateRepository readStates,
            UserRepository users,
            ChatAccessService access,
            CurrentUserService currentUser,
            RealtimePublisher realtime
    ) {
        this.channels = channels;
        this.messages = messages;
        this.readStates = readStates;
        this.users = users;
        this.access = access;
        this.currentUser = currentUser;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> visibleChannels(Authentication authentication) {
        Long userId = currentUser.requireUserId(authentication);
        List<ChannelResponse> result = new ArrayList<>();
        for (ChatChannelEntity channel : channels.findAllByOrderByNameAsc()) {
            if (access.canAccess(channel, authentication)) {
                Instant readAt = readStates.findById(new ChatReadStateId(userId, channel.getId()))
                        .map(ChatReadStateEntity::getReadAt)
                        .orElse(Instant.EPOCH);
                long unread = readAt.equals(Instant.EPOCH)
                        ? messages.countByChannelId(channel.getId())
                        : messages.countByChannelIdAndSentAtAfter(channel.getId(), readAt);
                result.add(new ChannelResponse(channel.getId(), channel.getName(), channel.getRequiredRole(), unread));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(
            UUID channelId, String before, int limit, Authentication authentication
    ) {
        access.requireAccessible(channelId, authentication);
        TimeUuidCursor cursor = before == null ? TimeUuidCursor.beforeNow() : TimeUuidCursor.parse(before);
        List<ChatMessageResponse> result = new ArrayList<>(messages
                .findBefore(
                        channelId, cursor.timestamp(), cursor.id(), PageRequest.of(0, Math.clamp(limit, 1, 100))
                )
                .stream().map(ChatMessageResponse::from).toList());
        Collections.reverse(result);
        return result;
    }

    @Transactional
    public ChatMessageResponse send(ChatSendRequest request, Authentication authentication) {
        Long senderId = currentUser.requireUserId(authentication);
        ChatChannelEntity channel = access.requireAccessible(request.channelId(), authentication);
        ChatMessageEntity existing = messages.findBySenderIdAndClientMessageId(senderId, request.clientMessageId())
                .orElse(null);
        if (existing != null) {
            return ChatMessageResponse.from(existing);
        }
        UserEntity sender = users.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        String body = request.body().trim();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Message body cannot be empty");
        }
        ChatMessageEntity saved;
        try {
            saved = messages.save(new ChatMessageEntity(request.clientMessageId(), channel, sender, body));
            messages.flush();
        } catch (DataIntegrityViolationException duplicate) {
            saved = messages.findBySenderIdAndClientMessageId(senderId, request.clientMessageId())
                    .orElseThrow(() -> duplicate);
        }
        ChatMessageResponse response = ChatMessageResponse.from(saved);
        realtime.afterCommitToDestination(
                "/topic/chat/" + channel.getId(), "chat.message.created", response
        );
        return response;
    }

    @Transactional
    public void markRead(UUID channelId, MarkReadRequest request, Authentication authentication) {
        Long userId = currentUser.requireUserId(authentication);
        ChatChannelEntity channel = access.requireAccessible(channelId, authentication);
        ChatMessageEntity message = messages.findById(request.lastReadMessageId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat message was not found"));
        if (!message.getChannel().getId().equals(channelId)) {
            throw new ForbiddenOperationException("The read cursor does not belong to this channel");
        }
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        ChatReadStateId id = new ChatReadStateId(userId, channelId);
        ChatReadStateEntity state = readStates.findById(id)
                .orElseGet(() -> new ChatReadStateEntity(user, channel));
        state.markRead(message);
        readStates.save(state);
    }
}
