package com.ntu.cloudgui.aggservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConfig - HikariCP Connection Pool Manager
 * 
 * Manages MySQL database connections with connection pooling.
 * Creates and maintains a pool of 5-20 connections for efficient
 * database access across multiple concurrent requests.
 * 
 * Responsibilities:
 * - Initialize HikariCP connection pool
 * - Create database schema on startup
 * - Provide connections to repositories
 * - Handle graceful shutdown
 */
@Configuration
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    private HikariDataSource dataSource;
    private final AppConfig appConfig;
    
    /**
     * Constructor - Initialize database configuration
     * 
     * @param appConfig Application configuration with database credentials
     */
    @Autowired
    public DatabaseConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.dataSource = createHikariPool();
        initializeSchema();
    }
    
    /**
     * Create and configure HikariCP connection pool
     * 
     * Pool Settings:
     * - Maximum 20 concurrent connections
     * - Minimum 5 idle connections (always ready)
     * - 10 second connection timeout
     * - 5 minute idle timeout
     * 
     * @return Configured HikariDataSource
     */
    private HikariDataSource createHikariPool() {
        logger.info("Creating HikariCP connection pool...");
        
        HikariConfig config = new HikariConfig();
        
        // Database Connection Settings
        config.setJdbcUrl(buildJdbcUrl());
        config.setUsername(appConfig.getDbUser());
        config.setPassword(appConfig.getDbPassword());
        
        // Pool Size Settings
        config.setMaximumPoolSize(20);      // Max 20 connections
        config.setMinimumIdle(5);           // Keep 5 ready
        config.setIdleTimeout(300000);      // 5 minutes
        config.setConnectionTimeout(10000); // 10 seconds
        
        // Performance Settings
        config.setAutoCommit(true);         // Auto-commit by default
        config.setLeakDetectionThreshold(60000); // Warn if held >60s
        config.setConnectionTestQuery("SELECT 1");   // Health check query
        
        // Connection Name for Debugging
        config.setPoolName("AggService-DB-Pool");
        
        try {
            HikariDataSource pool = new HikariDataSource(config);
            logger.info("✓ Connection pool created successfully");
            return pool;
        } catch (Exception e) {
            logger.error("✗ Failed to create connection pool", e);
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }
    
    /**
     * Build JDBC URL from configuration
     * Format: jdbc:mysql://host:port/database
     * 
     * @return Complete JDBC URL
     */
    private String buildJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
            appConfig.getDbHost(),
            appConfig.getDbPort(),
            appConfig.getDbName()
        );
    }
    
    /**
     * Initialize database schema
     * 
     * Creates tables if they don't exist:
     * - file_metadata: Stores file information
     * - chunk_metadata: Stores chunk information and locations
     * 
     * Called once at startup automatically.
     */
    private void initializeSchema() {
        logger.info("Initializing database schema...");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create file_metadata table
            String fileMetadataTable = 
                "CREATE TABLE IF NOT EXISTS file_metadata (" +
                "  fileId VARCHAR(36) PRIMARY KEY," +
                "  originalName VARCHAR(255) NOT NULL," +
                "  totalChunks INT NOT NULL," +
                "  sizeBytes BIGINT NOT NULL," +
                "  encryptionAlgo VARCHAR(50) DEFAULT 'AES/256/GCM'," +
                "  uploadTimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_timestamp (uploadTimestamp)" +
                ")";
            
            stmt.execute(fileMetadataTable);
            logger.info("✓ file_metadata table ready");
            
            // Create chunk_metadata table
            String chunkMetadataTable = 
                "CREATE TABLE IF NOT EXISTS chunk_metadata (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  fileId VARCHAR(36) NOT NULL," +
                "  chunkIndex INT NOT NULL," +
                "  serverHost VARCHAR(50) NOT NULL," +
                "  remotePath VARCHAR(255) NOT NULL," +
                "  crc32Checksum BIGINT NOT NULL," +
                "  sizeBytes BIGINT NOT NULL," +
                "  uploadTimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE KEY uk_file_chunk (fileId, chunkIndex)," +
                "  FOREIGN KEY (fileId) REFERENCES file_metadata(fileId) ON DELETE CASCADE," +
                "  INDEX idx_fileId (fileId)" +
                ")";
            
            stmt.execute(chunkMetadataTable);
            logger.info("✓ chunk_metadata table ready");
            
        } catch (SQLException e) {
            logger.error("✗ Failed to initialize database schema", e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
    
    /**
     * Get a connection from the pool
     * 
     * Returns a connection from HikariCP pool. Connection should be
     * returned to pool by calling close() in a finally block.
     * 
     * Example:
     * <pre>
     * Connection conn = dbConfig.getConnection();
     * try {
     *     // Use connection...
     * } finally {
     *     conn.close();  // Return to pool
     * }
     * </pre>
     * 
     * @return Connection from the pool
     * @throws SQLException if no connection available
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool is not initialized or closed");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Get HikariDataSource directly
     * 
     * Used by repositories to get the data source.
     * 
     * @return HikariDataSource instance
     */
    @Bean
    public HikariDataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Check if connection pool is healthy
     * 
     * @return true if pool is initialized and not closed
     */
    public boolean isHealthy() {
        return dataSource != null && !dataSource.isClosed();
    }
    
    /**
     * Graceful shutdown - close all connections
     * 
     * Called at application shutdown.
     * Closes all connections in pool and releases resources.
     */
    @PreDestroy
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool...");
            dataSource.close();
            logger.info("✓ Connection pool closed");
        }
    }
}
