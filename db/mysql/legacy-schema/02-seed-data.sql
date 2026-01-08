-- Seed data for development and testing

USE dbtutorial;

-- Insert default admin user (password: admin123 - should be hashed in production)
INSERT INTO users (username, password_hash, role) VALUES
('admin', SHA2('admin123', 256), 'admin'),
('testuser', SHA2('test123', 256), 'standard');

-- Initialize storage nodes
INSERT INTO storage_node_health (node_name, status, available_space_mb, used_space_mb) VALUES
('soft40051-files-container1', 'online', 10240, 0),
('soft40051-files-container2', 'online', 10240, 0),
('soft40051-files-container3', 'online', 10240, 0),
('soft40051-files-container4', 'online', 10240, 0);

-- Log system initialization
INSERT INTO system_logs (event_type, description, severity, source_component) VALUES
('System Initialization', 'Database schema initialized successfully', 'INFO', 'MySQL'),
('Storage Nodes Ready', 'All 4 storage nodes registered and online', 'INFO', 'System');
