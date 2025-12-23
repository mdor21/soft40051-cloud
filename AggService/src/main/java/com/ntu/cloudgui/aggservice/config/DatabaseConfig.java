package com.ntu.cloudgui.aggservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConfig - HikariCP Connection Pool Manager.
 *
 * Manages MySQL database connections with connection pooling.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private final AppConfig appConfig;
    private final HikariDataSource dataSource;

    public DatabaseConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.dataSource = createHikariPool();
        initializeSchema();
    }

    /**
     * Create and configure HikariCP connection pool.
     */
    private HikariDataSource createHikariPool() {
        logger.info("Creating HikariCP connection pool...");

        HikariConfig config = new HikariConfig();

        // Connection settings
        config.setJdbcUrl(buildJdbcUrl());
        config.setUsername(appConfig.getDbUser());
        config.setPassword(appConfig.getDbPassword());

        // Pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300_000);      // 5 minutes
        config.setConnectionTimeout(10_000); // 10 seconds

        // Performance / diagnostics
        config.setAutoCommit(true);
        config.setLeakDetectionThreshold(60_000);      // 60 seconds
        config.setConnectionTestQuery("SELECT 1");
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
     * Build JDBC URL from configuration.
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
     * Initialize database schema if tables do not exist.
     */
    private void initializeSchema() {
        logger.info("Initializing database schema...");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

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
     * Get a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool is not initialized or closed");
        }
        return dataSource.getConnection();
    }

    /**
     * Expose HikariDataSource as a Spring bean.
     */
    @Bean
    public HikariDataSource dataSource() {
        return dataSource;
    }

    /**
     * Check if connection pool is healthy.
     */
    public boolean isHealthy() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Graceful shutdown - close all connections.
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
