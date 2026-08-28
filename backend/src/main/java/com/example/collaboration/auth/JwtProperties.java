package com.example.collaboration.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        long accessTokenMinutes,
        String publicKey,
        String privateKey,
        boolean requireConfiguredKeys
) {
}

