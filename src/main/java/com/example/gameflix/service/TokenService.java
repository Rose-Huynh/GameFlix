package com.example.gameflix.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/*
Description: Creates signed JWTs after successful authentication.
Includes the user ID, issuer, issue time, and expiration
without placing passwords or profile details in the token.

Author: Rose
Date: October 18, 2026
*/

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final Duration lifetime;

    // Read the token lifetime from configuration and reject nonpositive values.
    public TokenService(JwtEncoder encoder, @Value("${app.jwt.lifetime}") Duration lifetime) {
        if (lifetime.isNegative() || lifetime.isZero()) throw new IllegalArgumentException("JWT lifetime must be positive");
        this.encoder = encoder;
        this.lifetime = lifetime;
    }

    public record Token(String token, String tokenType, long expiresIn) {}

    // Include only the user ID, issuer, and timestamps, then sign with HS256.
    public Token issue(int userId) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("gameflix").subject(Integer.toString(userId))
                .issuedAt(now).expiresAt(now.plus(lifetime)).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new Token(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
                "Bearer", lifetime.toSeconds());
    }
}
