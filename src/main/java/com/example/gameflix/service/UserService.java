package com.example.gameflix.service;

import com.example.gameflix.model.User;
import com.example.gameflix.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/*
Description: Handles registration and credential verification.
Validates input, normalizes emails, hashes passwords,
and handles duplicate email constraint violations.

Author: Rose
Date: October 18, 2026
*/

@Service
public class UserService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final String dummyHash;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
        // Still perform a hash check for unknown emails to reduce timing differences.
        this.dummyHash = encoder.encode("unknown-user-timing-placeholder");
    }

    // Check email format and both the character and UTF-8 byte limits for passwords.
    public boolean valid(String email, String password) {
        return email != null && email.strip().length() <= 255
                && EMAIL.matcher(email.strip()).matches()
                && password != null && !password.isBlank()
                && password.length() <= 64
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // Hash before saving; flushing makes database constraint failures surface here.
    public boolean register(String email, String password) {
        email = normalizeEmail(email);
        try {
            repository.saveAndFlush(new User(email, encoder.encode(password)));
            return true;
        } catch (DataIntegrityViolationException exception) {
            // Only the email unique constraint is a duplicate registration.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_users_email")) {
                    return false;
                }
            }
            throw exception;
        }
    }

    // Verify the submitted password against the stored hash without decrypting it.
    public java.util.Optional<User> login(String email, String password) {
        var user = repository.findByEmail(normalizeEmail(email));
        boolean matches = encoder.matches(password, user.map(User::getPasswordHash).orElse(dummyHash));
        return matches ? user : java.util.Optional.empty();
    }

    // Use the same email normalization for registration and login.
    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
