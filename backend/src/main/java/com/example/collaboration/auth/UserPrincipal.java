package com.example.collaboration.auth;

import com.example.collaboration.user.Role;
import com.example.collaboration.user.UserEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(
        Long id,
        String email,
        String password,
        String displayName,
        Role role
) implements UserDetails {

    public static UserPrincipal from(UserEntity user) {
        return new UserPrincipal(
                user.getId(), user.getEmail(), user.getPasswordHash(), user.getDisplayName(), user.getRole()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return password; }
}

