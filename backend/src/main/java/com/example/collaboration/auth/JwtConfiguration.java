package com.example.collaboration.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JwtConfiguration.class);

    @Bean
    KeyPair jwtKeyPair(JwtProperties properties) {
        boolean hasPublic = hasText(properties.publicKey());
        boolean hasPrivate = hasText(properties.privateKey());
        if (hasPublic != hasPrivate) {
            throw new IllegalStateException("Both JWT_PUBLIC_KEY and JWT_PRIVATE_KEY must be configured together");
        }
        if (hasPublic) {
            return loadKeyPair(properties.publicKey(), properties.privateKey());
        }
        if (properties.requireConfiguredKeys()) {
            throw new IllegalStateException("Configured RSA JWT keys are required in this environment");
        }
        log.warn("JWT keys are not configured; using an ephemeral development RSA key");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate development JWT key pair", exception);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair keyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(KeyPair keyPair, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "The required audience is missing", null
                ));
        OAuth2TokenValidator<Jwt> applicationClaims = token -> validApplicationClaims(token)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "Required application claims are invalid", null
                ));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience, applicationClaims));
        return decoder;
    }

    private boolean validApplicationClaims(Jwt token) {
        String subject = token.getSubject();
        String role = token.getClaimAsString("role");
        if (subject == null || !subject.matches("[1-9][0-9]*")
                || role == null || !Set.of("ADMIN", "MANAGER", "USER").contains(role)) {
            return false;
        }
        try {
            return Long.parseLong(subject) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private KeyPair loadKeyPair(String publicKey, String privateKey) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicRsa = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(decodePem(publicKey, "PUBLIC KEY"))
            );
            RSAPrivateKey privateRsa = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(decodePem(privateKey, "PRIVATE KEY"))
            );
            return new KeyPair(publicRsa, privateRsa);
        } catch (Exception exception) {
            throw new IllegalStateException("JWT key configuration is invalid", exception);
        }
    }

    private byte[] decodePem(String value, String label) {
        String normalized = value
                .replace("\\n", "\n")
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
