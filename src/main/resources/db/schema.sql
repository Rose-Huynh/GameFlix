/*
Description: Defines the MySQL users table for a new database.
Includes an auto-generated ID, optional username, unique email,
and a VARCHAR(255) column for password hashes.
This reference schema is not automatically executed.

Author: Rose
Date: October 18, 2026
*/
CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT 'Adaptive password hash only',
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);
