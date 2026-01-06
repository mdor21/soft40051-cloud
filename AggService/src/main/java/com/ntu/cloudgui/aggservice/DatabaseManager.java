package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final int MAX_POOL_SIZE = 10;
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;

    private final BlockingQueue<Connection> connectionPool;
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPass;

    public DatabaseManager(Configuration config) {
        this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s",
                config.getDbHost(), config.getDbPort(), config.getDbName());
        this.dbUser = config.getDbUser();
        this.dbPass = config.getDbPass();
        this.connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        initializePool();
    }

    private void initializePool() {
        try {
            for (int i = 0; i < MAX_POOL_SIZE; i++) {
                Connection connection = createConnection();
                connectionPool.offer(connection);
            }
            logger.info("Database connection pool initialized with {} connections", MAX_POOL_SIZE);
        } catch (SQLException e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection connection = connectionPool.poll(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (connection == null || connection.isClosed()) {
                logger.warn("Connection pool timeout or connection is closed, creating new connection");
                connection = createConnection();
            }
            return connection;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a database connection", e);
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connectionPool.offer(connection);
                } else {
                    logger.warn("Released connection is closed, creating a new one");
                    connectionPool.offer(createConnection());
                }
            } catch (SQLException e) {
                logger.error("Failed to check if connection is closed", e);
            }
        }
    }

    public void closePool() {
        for (Connection connection : connectionPool) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Error closing connection in pool", e);
            }
        }
        logger.info("Database connection pool closed");
    }
}
