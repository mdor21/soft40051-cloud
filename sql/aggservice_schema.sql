CREATE DATABASE IF NOT EXISTS aggservice_db;

USE aggservice_db;

CREATE TABLE IF NOT EXISTS file_metadata (
    file_id VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    total_chunks INT NOT NULL,
    size_bytes BIGINT NOT NULL,
    encryption_algo VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id)
);

CREATE TABLE IF NOT EXISTS chunk_metadata (
    chunk_id BIGINT AUTO_INCREMENT,
    file_id VARCHAR(255) NOT NULL,
    chunk_index INT NOT NULL,
    server_host VARCHAR(255) NOT NULL,
    remote_path VARCHAR(255) NOT NULL,
    original_size BIGINT NOT NULL,
    encrypted_size BIGINT NOT NULL,
    crc32 BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chunk_id),
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS logs (
    id BIGINT AUTO_INCREMENT,
    level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
