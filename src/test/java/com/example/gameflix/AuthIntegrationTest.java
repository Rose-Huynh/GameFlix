package com.example.gameflix;

import com.example.gameflix.model.User;
import com.example.gameflix.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:auth-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @DynamicPropertySource
    static void testSecret(DynamicPropertyRegistry properties) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String secret = Base64.getEncoder().encodeToString(key);
        properties.add("app.jwt.secret", () -> secret);
        properties.add("app.frontend-origin", () -> "http://localhost:3000");
    }

    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach void clearUsers() { users.deleteAll(); }

    private void request(String path, String body, int status, String message) throws Exception {
        var result = mvc.perform(post(path).contentType("application/json").content(body))
                .andExpect(status().is(status));
        if (path.endsWith("/login") && status == 200) {
            result.andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(900));
        } else {
            result.andExpect(jsonPath("$.message").value(message));
        }
    }

    @Test void registrationAndLoginFlowStoresOnlyHash() throws Exception {
        String credentials = "{\"email\":\"gameuser@example.com\",\"password\":\"gameflix123\"}";
        request("/api/auth/register", credentials, 201, "User registered successfully");
        String hash = users.findByEmail("gameuser@example.com").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("{bcrypt}$2a$12$").isNotEqualTo("gameflix123");
        assertThat(encoder.matches("gameflix123", hash)).isTrue();
        request("/api/auth/register", credentials, 409, "Email already registered");
        assertThat(users.count()).isEqualTo(1);
        request("/api/auth/login", credentials, 200, "Login successful");
        request("/api/auth/login", "{\"email\":\"gameuser@example.com\",\"password\":\"wrong\"}", 401, "Invalid email or password");
        request("/api/auth/login", "{\"email\":\"missing@example.com\",\"password\":\"gameflix123\"}", 401, "Invalid email or password");
    }

    @Test void invalidInputsAreRejectedWithoutStoringUsers() throws Exception {
        for (String body : new String[]{"{}", "{\"email\":\" \",\"password\":\"abc\"}",
                "{\"email\":\"not-an-email\",\"password\":\"abc\"}",
                "{\"username\":\"olduser\",\"password\":\"abc\"}",
                "{\"email\":\"user@example.com\",\"password\":\"\"}",
                "{\"email\":\"user@example.com\",\"password\":\"" + "é".repeat(37) + "\"}"}) {
            mvc.perform(post("/api/auth/register").contentType("application/json").content(body)).andExpect(status().isBadRequest());
            request("/api/auth/login", body, 401, "Invalid email or password");
        }
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{bad" )).andExpect(status().isBadRequest());
        assertThat(users.count()).isZero();
    }

    @Test void emailIsNormalizedForRegistrationAndLogin() throws Exception {
        String credentials = "{\"email\":\" GameUser@Example.com \",\"password\":\"gameflix123\"}";
        request("/api/auth/register", credentials, 201, "User registered successfully");
        assertThat(users.findByEmail("gameuser@example.com")).isPresent();
        String normalized = "{\"email\":\"gameuser@example.com\",\"password\":\"gameflix123\"}";
        request("/api/auth/register", normalized, 409, "Email already registered");
        request("/api/auth/login", credentials, 200, "Login successful");
        request("/api/auth/login", normalized, 200, "Login successful");
        assertThat(users.count()).isEqualTo(1);
    }

    @Test void protectedProfileRequiresValidUnexpiredSignature() throws Exception {
        String credentials = "{\"email\":\"owner@example.com\",\"password\":\"gameflix123\"}";
        request("/api/auth/register", credentials, 201, "User registered successfully");
        users.saveAndFlush(new User("other@example.com", encoder.encode("different")));
        String response = mvc.perform(post("/api/auth/login").contentType("application/json").content(credentials))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("token").asText();
        Jwt decoded = jwtDecoder.decode(token);
        assertThat(decoded.getExpiresAt()).isAfter(Instant.now());
        assertThat(decoded.getClaims()).containsOnlyKeys("iss", "sub", "iat", "exp");
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.id").value(users.findByEmail("owner@example.com").orElseThrow().getId()))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        String[] parts = token.split("\\.");
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"999999\",\"exp\":4102444800,\"iss\":\"gameflix\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + payload + "." + parts[2];
        for (String invalid : new String[]{tampered, "not-a-jwt", signedToken(Instant.now().minusSeconds(120), true),
                signedToken(Instant.now().plusSeconds(120), false)}) {
            mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String signedToken(Instant expiration, boolean includeExpiration) {
        var claims = JwtClaimsSet.builder().issuer("gameflix").subject("1").issuedAt(Instant.now().minusSeconds(300));
        if (includeExpiration) claims.expiresAt(expiration);
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    @Test void passwordLengthBoundaries() throws Exception {
        for (String password : new String[]{"a".repeat(65), "é".repeat(37)}) {
            request("/api/auth/register", json.writeValueAsString(java.util.Map.of("email", "limit@example.com", "password", password)),
                    400, "A valid email and password are required; email must be at most 255 characters and password at most 64 characters and 72 UTF-8 bytes");
        }
        for (String password : new String[]{"a".repeat(64), "é".repeat(36)}) {
            users.deleteAll();
            request("/api/auth/register", json.writeValueAsString(java.util.Map.of("email", "limit@example.com", "password", password)),
                    201, "User registered successfully");
        }
    }

    @Test void corsAllowsOnlyConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/users/me").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(options("/api/users/me").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void legacyRawBcryptHashesStillVerify() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(10).encode("legacy");
        assertThat(encoder.matches("legacy", hash)).isTrue();
    }

    @Test void databaseRejectsDuplicateEmailsWithoutServiceChecks() {
        String hash = encoder.encode("gameflix123");
        users.saveAndFlush(new User("gameuser@example.com", hash));

        assertThatThrownBy(() -> users.saveAndFlush(new User("gameuser@example.com", hash)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(users.count()).isEqualTo(1);
    }
}
