package com.example.collaboration.chat;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MarkReadRequest(@NotNull UUID lastReadMessageId) {
}

