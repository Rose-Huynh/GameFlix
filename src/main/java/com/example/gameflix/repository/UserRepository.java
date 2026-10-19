package com.example.gameflix.repository;

import com.example.gameflix.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/*
Description: Provides database access for User entities through Spring Data JPA.
Supports saving users, finding users by ID,
and looking up users by email.

Author: Rose
Date: October 19, 2026
*/

// Inherit standard save, delete, and ID lookup operations from Spring Data JPA.
public interface UserRepository extends JpaRepository<User, Integer> {
    // Spring generates the email lookup; Optional is empty if no user matches.
    Optional<User> findByEmail(String email);
}
