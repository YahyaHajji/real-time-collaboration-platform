package com.example.collaboration.user;

public record UserSummaryResponse(Long id, String email, String displayName, Role role) {
    public static UserSummaryResponse from(UserEntity user) {
        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}

