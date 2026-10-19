package com.example.gameflix.controller;

import com.example.gameflix.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
Description: Provides the protected current-user profile endpoint.
Uses the validated JWT subject to find the caller
and returns only their ID and email.

Author: Rose
Date: October 18, 2026
*/

@RestController
public class UserController {
    private final UserRepository users;

    public UserController(UserRepository users) { this.users = users; }

    // Expose only safe profile fields, never the stored password hash.
    public record Profile(int id, String email) {}

    // Identify the caller using the token already verified by Spring Security.
    @GetMapping("/api/users/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        try {
            return users.findById(Integer.parseInt(jwt.getSubject()))
                    .<ResponseEntity<?>>map(user -> ResponseEntity.ok(new Profile(user.getId(), user.getEmail())))
                    .orElseGet(() -> ResponseEntity.status(401).body(new AuthController.Message("Unauthorized")));
        } catch (NumberFormatException exception) {
            return ResponseEntity.status(401).body(new AuthController.Message("Unauthorized"));
        }
    }
}
