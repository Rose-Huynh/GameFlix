package com.example.gameflix.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
Description: Provides a BCrypt password encoder managed by Spring.
UserService uses it to hash passwords during registration
and check passwords against stored hashes during login.

Author: Rose
Date: October 18, 2026
*/

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Store algorithm-prefixed BCrypt hashes with a deliberate cost of 12.
        var bcrypt = new BCryptPasswordEncoder(12);
        var encoder = new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", bcrypt));
        // Also verify older BCrypt hashes that have no algorithm prefix.
        encoder.setDefaultPasswordEncoderForMatches(bcrypt);
        return encoder;
    }
}
