package com.example.collaboration.chat;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels")
public class ChatRestController {

    private final ChatService chat;

    public ChatRestController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    List<ChannelResponse> channels(Authentication authentication) {
        return chat.visibleChannels(authentication);
    }

    @GetMapping("/{channelId}/messages")
    List<ChatMessageResponse> history(
            @PathVariable UUID channelId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication
    ) {
        return chat.history(channelId, before, limit, authentication);
    }

    @PutMapping("/{channelId}/read-cursor")
    ResponseEntity<Void> markRead(
            @PathVariable UUID channelId,
            @Valid @RequestBody MarkReadRequest request,
            Authentication authentication
    ) {
        chat.markRead(channelId, request, authentication);
        return ResponseEntity.noContent().build();
    }
}
