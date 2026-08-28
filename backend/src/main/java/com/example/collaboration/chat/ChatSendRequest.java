package com.example.collaboration.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChatSendRequest(
        @NotNull UUID channelId,
        @NotNull UUID clientMessageId,
        @NotBlank @Size(max = 2000) String body
) {
}

