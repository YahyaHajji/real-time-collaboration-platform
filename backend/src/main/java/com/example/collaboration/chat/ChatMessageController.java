package com.example.collaboration.chat;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import com.example.collaboration.realtime.RealtimeCommandError;

@Controller
public class ChatMessageController {

    private final ChatService chat;

    public ChatMessageController(ChatService chat) {
        this.chat = chat;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid ChatSendRequest request, Authentication authentication) {
        chat.send(request, authentication);
    }

    @MessageExceptionHandler({IllegalArgumentException.class, jakarta.validation.ValidationException.class})
    @SendToUser("/queue/errors")
    public RealtimeCommandError invalidCommand(RuntimeException exception) {
        return RealtimeCommandError.safe("The chat command was rejected");
    }
}
