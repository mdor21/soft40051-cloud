-- User Profiles (Master Record)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    role ENUM('admin', 'standard') NOT NULL DEFAULT 'standard',
    encrypted_password VARCHAR(255) NOT NULL,
    created_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    modified_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- File Metadata (Master Record)
CREATE TABLE IF NOT EXISTS files (
    file_id VARCHAR(255) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    file_size BIGINT,
    created_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    modified_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner) REFERENCES users(username),
    INDEX idx_owner (owner),
    INDEX idx_filename (filename)
);

-- Chunk Metadata (Master Record)
CREATE TABLE IF NOT EXISTS chunks (
    chunk_id VARCHAR(255) PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL,
    chunk_index INT NOT NULL,
    server_location VARCHAR(255) NOT NULL,
    crc32_checksum VARCHAR(8) NOT NULL,
    created_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE,
    INDEX idx_file_id (file_id),
    INDEX idx_server_location (server_location)
);

-- Access Control Lists (Master Record)
CREATE TABLE IF NOT EXISTS acls (
    acl_id VARCHAR(255) PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL,
    shared_with VARCHAR(255) NOT NULL,
    permission_type ENUM('read', 'write', 'read_write') NOT NULL,
    created_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE,
    FOREIGN KEY (shared_with) REFERENCES users(username),
    INDEX idx_file_id (file_id),
    INDEX idx_shared_with (shared_with)
);

-- Centralised Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    service VARCHAR(100) NOT NULL,
    log_level ENUM('INFO', 'WARN', 'ERROR', 'DEBUG') NOT NULL DEFAULT 'INFO',
    username VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    details TEXT,
    stack_trace LONGTEXT,
    INDEX idx_timestamp (timestamp),
    INDEX idx_service (service),
    INDEX idx_username (username),
    INDEX idx_log_level (log_level)
);

-- Insert default admin user
INSERT IGNORE INTO users (username, role, encrypted_password) 
VALUES ('admin', 'admin', SHA2('admin', 256));
