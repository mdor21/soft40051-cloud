-- AggService Database Schema
-- This script defines the required tables for user profiles, ACLs, and audit logs.

-- User Profiles Table
-- Stores user information and roles.
CREATE TABLE IF NOT EXISTS `user_profiles` (
  `user_id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(255) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` ENUM('Standard', 'Admin') NOT NULL DEFAULT 'Standard',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Access Control List (ACL) Table
-- Manages file sharing permissions between users.
CREATE TABLE IF NOT EXISTS `acl` (
  `acl_id` INT AUTO_INCREMENT PRIMARY KEY,
  `file_id` BIGINT NOT NULL,
  `user_id` INT NOT NULL,
  `permission` ENUM('read', 'write') NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`file_id`) REFERENCES `file_metadata`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `user_profiles`(`user_id`) ON DELETE CASCADE
);

-- Audit Logs Table
-- Provides a comprehensive audit trail of all system events.
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `log_id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(255),
  `event_type` VARCHAR(255) NOT NULL,
  `event_description` TEXT,
  `status` ENUM('SUCCESS', 'FAILURE') NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
