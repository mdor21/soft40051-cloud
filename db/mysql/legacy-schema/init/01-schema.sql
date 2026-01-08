-- SOFT40051 Cloud Simulation Database Schema
-- Database: dbtutorial

USE dbtutorial;

-- User Profiles Table
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
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
    encryption_algorithm VARCHAR(50) DEFAULT 'AES',
    total_chunks INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    INDEX idx_user_files (user_id),
    INDEX idx_filename (original_filename)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- File Chunks Table
CREATE TABLE IF NOT EXISTS file_chunks (
    chunk_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    sequence_order INT NOT NULL,
    chunk_size INT NOT NULL,
    crc32_checksum VARCHAR(8) NOT NULL,
    server_location VARCHAR(100) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE CASCADE,
    UNIQUE KEY unique_file_chunk (file_id, sequence_order),
    INDEX idx_file_chunks (file_id, sequence_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Access Control List (ACL) Table
CREATE TABLE IF NOT EXISTS access_control_list (
    acl_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    user_id INT NOT NULL,
    permission_type ENUM('read', 'write', 'read_write') NOT NULL,
    granted_by INT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES user_profiles(user_id),
    UNIQUE KEY unique_file_user_acl (file_id, user_id),
    INDEX idx_user_permissions (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- System Logs Table
CREATE TABLE IF NOT EXISTS system_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(100) NOT NULL,
    user_id INT NULL,
    file_id INT NULL,
    description TEXT,
    severity ENUM('INFO', 'WARNING', 'ERROR', 'CRITICAL') DEFAULT 'INFO',
    source_component VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE SET NULL,
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE SET NULL,
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Load Balancer Statistics Table
CREATE TABLE IF NOT EXISTS load_balancer_stats (
    stat_id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    algorithm_used ENUM('FCFS', 'SJN', 'RoundRobin') NOT NULL,
    simulated_latency_seconds DECIMAL(3,1) NOT NULL,
    task_type VARCHAR(50),
    user_id INT,
    queue_length INT,
    FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE SET NULL,
    INDEX idx_algorithm (algorithm_used),
    INDEX idx_timestamp_lb (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Storage Node Health Table
CREATE TABLE IF NOT EXISTS storage_node_health (
    health_id INT AUTO_INCREMENT PRIMARY KEY,
    node_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('online', 'offline', 'degraded') NOT NULL,
    available_space_mb BIGINT,
    chunk_count INT DEFAULT 0,
    last_heartbeat TIMESTAMP NULL,
    INDEX idx_node_status (node_name, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
