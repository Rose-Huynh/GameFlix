package com.example.gameflix.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

/*
Description: Configures JWT authentication and protected routes.
Loads the signing key, validates incoming tokens, restricts CORS,
and returns 401 responses for authentication failures.

Author: Rose
Date: October 18, 2026
*/

@Configuration
public class SecurityConfig {
    // Load the external signing key and require at least 32 bytes.
    @Bean
    SecretKey jwtKey(@Value("${app.jwt.secret}") String encoded) {
        byte[] key = Base64.getDecoder().decode(encoded);
        if (key.length < 32) throw new IllegalArgumentException("JWT key must be at least 32 random bytes, Base64 encoded");
        return new SecretKeySpec(key, "HmacSHA256");
    }

    // Sign outgoing JWTs with the configured key.
    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    // Verify signatures, expiration, issuer, and the user ID claim.
    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> required = jwt ->
                jwt.getExpiresAt() != null && jwt.getSubject() != null && jwt.getSubject().matches("[1-9][0-9]*")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator("gameflix"), required));
        return decoder;
    }

    // Return a generic JSON 401 response when authentication fails.
    @Bean
    AuthenticationEntryPoint unauthorized() {
        return (request, response, error) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
        };
    }

    // Allow browser requests only from the configured front-end origin.
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin}") String origin) {
        if (origin.contains("*")) throw new IllegalArgumentException("Configure a specific frontend origin");
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    // Keep registration and login public; require bearer tokens for other routes.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint unauthorized) throws Exception {
        // Stateless bearer authentication does not use browser login cookies.
        return http.cors(cors -> {}).csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}).authenticationEntryPoint(unauthorized))
                .build();
    }
}
