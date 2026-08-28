package com.example.collaboration.auth;

import com.example.collaboration.user.UserEntity;
import com.example.collaboration.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository users,
            JwtEncoder encoder,
            JwtProperties properties
    ) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email().trim().toLowerCase(Locale.ROOT), request.password()
                )
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserEntity user = users.findById(principal.id()).orElseThrow();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("role", user.getRole().name())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims
        )).getTokenValue();
        return new AuthResponse(
                token, "Bearer", expiresAt, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole()
        );
    }
}

