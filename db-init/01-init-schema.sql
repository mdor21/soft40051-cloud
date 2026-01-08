-- Database initialization for SOFT40051 Cloud Storage System
-- This script creates all required tables for the distributed file storage system

USE dbtutorial;

-- User Profiles Table
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'standard') DEFAULT 'standard',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- File Metadata Table
CREATE TABLE IF NOT EXISTS file_metadata (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    encryption_algorithm VARCHAR(50) DEFAULT 'AES-256',
    total_chunks INT NOT NULL,
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status ENUM('uploading', 'complete', 'corrupted', 'deleted') DEFAULT 'uploading',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_files (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chunk Information Table
CREATE TABLE IF NOT EXISTS file_chunks (
    chunk_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    sequence_order INT NOT NULL,
    chunk_size INT NOT NULL,
    crc32_checksum VARCHAR(8) NOT NULL,
    server_location VARCHAR(100) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE CASCADE,
    UNIQUE KEY unique_file_sequence (file_id, sequence_order),
    INDEX idx_file_chunks (file_id),
    INDEX idx_server_location (server_location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Access Control List (ACL)
CREATE TABLE IF NOT EXISTS file_permissions (
    permission_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    user_id INT NOT NULL,
    permission_type ENUM('read', 'write', 'owner') NOT NULL,
    granted_by INT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES users(user_id),
    UNIQUE KEY unique_file_user_permission (file_id, user_id, permission_type),
    INDEX idx_user_permissions (user_id),
    INDEX idx_file_permissions (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- System Logs Table
CREATE TABLE IF NOT EXISTS system_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(100) NOT NULL,
    user_id INT NULL,
    file_id INT NULL,
    description TEXT,
    severity ENUM('INFO', 'WARNING', 'ERROR', 'CRITICAL') DEFAULT 'INFO',
    source_component VARCHAR(50),
    ip_address VARCHAR(45),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE SET NULL,
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_user_logs (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Load Balancer Statistics Table
CREATE TABLE IF NOT EXISTS load_balancer_stats (
    stat_id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scheduling_algorithm ENUM('FCFS', 'SJN', 'RoundRobin') NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    simulated_latency DECIMAL(4,2) NOT NULL,
    queue_size INT,
    processing_time_ms INT,
    success BOOLEAN DEFAULT TRUE,
    INDEX idx_timestamp (timestamp),
    INDEX idx_algorithm (scheduling_algorithm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Storage Node Health Monitoring
CREATE TABLE IF NOT EXISTS storage_node_health (
    health_id INT AUTO_INCREMENT PRIMARY KEY,
    node_name VARCHAR(100) NOT NULL,
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status ENUM('online', 'offline', 'degraded') DEFAULT 'online',
    available_space_mb BIGINT,
    used_space_mb BIGINT,
    chunk_count INT DEFAULT 0,
    INDEX idx_node_status (node_name, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Database for Gitea (if not exists)
CREATE DATABASE IF NOT EXISTS gitea CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
