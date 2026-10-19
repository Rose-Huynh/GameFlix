package com.example.gameflix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
Description: Starts the GameFlix Spring Boot application.
Spring discovers and configures the controllers, services,
repositories, and application configuration.

Author: Rose
Date: October 18, 2026
*/

@SpringBootApplication
public class GameFlixApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameFlixApplication.class, args);
    }

}
