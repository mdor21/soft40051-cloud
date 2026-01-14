package com.ntu.cloudgui.app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnectionManager {

    // Read database connection details from environment variables
    private static final String HOST = getEnv("MYSQL_HOST", "DB_HOST", "lamp-server");
    private static final int PORT = Integer.parseInt(getEnv("MYSQL_PORT", "DB_PORT", "3306"));
    private static final String DB = getEnv("MYSQL_DATABASE", "DB_NAME", "dbtutorial");
    private static final String USER = getEnv("MYSQL_USER", "DB_USER", null);
    private static final String PASS = getEnv("MYSQL_PASSWORD", "DB_PASS", null);

    private static final String URL = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
            HOST, PORT, DB
    );

    public static Connection getConnection() throws SQLException {
        if (USER == null || PASS == null) {
            throw new SQLException("Database credentials (MYSQL_USER/MYSQL_PASSWORD or DB_USER/DB_PASS) are not set in the environment.");
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static String getEnv(String primaryKey, String fallbackKey, String defaultValue) {
        String value = System.getenv(primaryKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = System.getenv(fallbackKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }
}
