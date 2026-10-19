package com.example.gameflix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/*
Description: Represents a user stored in the MySQL users table.
Maps the user ID, optional username, unique email,
and password hash to database columns.

Author: Rose
Date: October 19, 2026
*/

// Map the entity to users and enforce email uniqueness in the database.
@Entity
@Table(name = "users", uniqueConstraints =
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    // MySQL generates the primary key when a user is inserted.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    // Store the encoded hash with enough space for its algorithm prefix.
    @Column(name = "password", nullable = false, length = 255)
    private String passwordHash;

    // JPA needs a no-argument constructor to load stored users.
    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
