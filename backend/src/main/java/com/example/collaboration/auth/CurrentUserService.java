package com.example.collaboration.auth;

import com.example.collaboration.common.ResourceNotFoundException;
import com.example.collaboration.user.UserEntity;
import com.example.collaboration.user.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public UserEntity requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResourceNotFoundException("Authenticated user was not found");
        }
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return find(principal.id());
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return find(parseUserId(jwt.getToken().getSubject()));
        }
        throw new ResourceNotFoundException("Authenticated user was not found");
    }

    public Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user was not found");
        }
        return parseUserId(authentication.getName());
    }

    private Long parseUserId(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new ResourceNotFoundException("Authenticated user was not found");
        }
    }

    private UserEntity find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }
}

