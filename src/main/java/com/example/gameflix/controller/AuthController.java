package com.example.gameflix.controller;

import com.example.gameflix.service.TokenService;
import com.example.gameflix.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
Description: Provides the registration and login REST endpoints.
Delegates credential checks to UserService and returns
a signed JWT after a successful login.

Author: Rose
Date: October 18, 2026
*/

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService users;

    private final TokenService tokens;

    public AuthController(UserService users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    // Define the JSON request fields and message response.
    public record Credentials(String email, String password) {}
    public record Message(String message) {}

    // Validate registration input and return 201, 400, or 409.
    @PostMapping("/register")
    public ResponseEntity<Message> register(@RequestBody Credentials credentials) {
        if (!users.valid(credentials.email(), credentials.password())) {
            return ResponseEntity.badRequest().body(new Message("A valid email and password are required; email must be at most 255 characters and password at most 64 characters and 72 UTF-8 bytes"));
        }
        if (!users.register(credentials.email(), credentials.password())) {
            return ResponseEntity.status(409).body(new Message("Email already registered"));
        }
        return ResponseEntity.status(201).body(new Message("User registered successfully"));
    }

    // Return a signed token on success or the same generic error for bad credentials.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials credentials) {
        if (!users.valid(credentials.email(), credentials.password())) {
            return ResponseEntity.status(401).body(new Message("Invalid email or password"));
        }
        var user = users.login(credentials.email(), credentials.password());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(new Message("Invalid email or password"));
        }
        return ResponseEntity.ok(tokens.issue(user.get().getId()));
    }
}
