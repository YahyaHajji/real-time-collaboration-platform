package com.example.collaboration.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotNull Long recipientUserId,
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9._-]{2,79}") String type,
        @NotBlank @Size(max = 500) String message
) {
}

