CREATE DATABASE IF NOT EXISTS dbtutorial;
USE dbtutorial;

-- User Profiles Table
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    role ENUM('admin', 'standard') DEFAULT 'standard',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- File Metadata Table
CREATE TABLE IF NOT EXISTS file_metadata (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    encryption_algorithm VARCHAR(50) DEFAULT 'AES-256',
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_files (user_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- File Chunks Table
CREATE TABLE IF NOT EXISTS file_chunks (
    chunk_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    chunk_sequence INT NOT NULL,
    crc32_checksum VARCHAR(8) NOT NULL,
    server_location VARCHAR(100) NOT NULL,
    chunk_size INT NOT NULL,
    stored_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE CASCADE,
    UNIQUE KEY unique_chunk (file_id, chunk_sequence),
    INDEX idx_file_chunks (file_id, chunk_sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Access Control List (ACL)
CREATE TABLE IF NOT EXISTS file_acl (
    acl_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    user_id INT NOT NULL,
    permission ENUM('read', 'write', 'read_write') NOT NULL,
    granted_by INT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES users(user_id),
    UNIQUE KEY unique_permission (file_id, user_id),
    INDEX idx_user_permissions (user_id)
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
    ip_address VARCHAR(45),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE SET NULL,
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type),
    INDEX idx_user_logs (user_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default admin user (password: admin123 - should be properly hashed)
INSERT INTO users (username, password_hash, role) VALUES 
('admin', SHA2('admin123', 256), 'admin'),
('testuser', SHA2('test123', 256), 'standard');

-- Create stored procedure for logging
DELIMITER //
CREATE PROCEDURE log_event(
    IN p_event_type VARCHAR(100),
    IN p_user_id INT,
    IN p_file_id INT,
    IN p_description TEXT,
    IN p_severity VARCHAR(20)
)
BEGIN
    INSERT INTO system_logs (event_type, user_id, file_id, description, severity)
    VALUES (p_event_type, p_user_id, p_file_id, p_description, p_severity);
END //
DELIMITER ;
