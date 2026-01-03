-- User Session Data
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL CHECK(role IN ('admin', 'standard')),
    encrypted_password TEXT NOT NULL,
    login_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_activity DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Temporary File Metadata (Cached from MySQL)
CREATE TABLE IF NOT EXISTS temp_files (
    file_id TEXT PRIMARY KEY,
    filename TEXT NOT NULL,
    owner TEXT NOT NULL,
    file_size INTEGER,
    created_timestamp DATETIME,
    modified_timestamp DATETIME,
    sync_status TEXT CHECK(sync_status IN ('synced', 'pending', 'conflict')) DEFAULT 'synced'
);

-- Temporary Chunk Metadata (Cached from MySQL)
CREATE TABLE IF NOT EXISTS temp_chunks (
    chunk_id TEXT PRIMARY KEY,
    file_id TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    server_location TEXT,
    crc32_checksum TEXT,
    sync_status TEXT CHECK(sync_status IN ('synced', 'pending', 'conflict')) DEFAULT 'synced',
    FOREIGN KEY (file_id) REFERENCES temp_files(file_id)
);

-- Sync State - Tracks pending changes
CREATE TABLE IF NOT EXISTS sync_queue (
    sync_id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT NOT NULL CHECK(entity_type IN ('user', 'file', 'chunk', 'acl')),
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL CHECK(operation IN ('create', 'update', 'delete')),
    data_json TEXT,
    created_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    synced BOOLEAN DEFAULT 0
);

-- Access Control Lists (Cached)
CREATE TABLE IF NOT EXISTS temp_acls (
    acl_id TEXT PRIMARY KEY,
    file_id TEXT NOT NULL,
    shared_with TEXT NOT NULL,
    permission_type TEXT NOT NULL CHECK(permission_type IN ('read', 'write', 'read_write')),
    sync_status TEXT CHECK(sync_status IN ('synced', 'pending', 'conflict')) DEFAULT 'synced',
    FOREIGN KEY (file_id) REFERENCES temp_files(file_id)
);

-- Local Logs (for debugging/audit trail when offline)
CREATE TABLE IF NOT EXISTS local_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    service TEXT NOT NULL,
    log_level TEXT NOT NULL CHECK(log_level IN ('INFO', 'WARN', 'ERROR', 'DEBUG')),
    message TEXT NOT NULL,
    stack_trace TEXT
);

CREATE INDEX IF NOT EXISTS idx_sync_queue_synced ON sync_queue(synced);
CREATE INDEX IF NOT EXISTS idx_local_logs_timestamp ON local_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_temp_files_owner ON temp_files(owner);
