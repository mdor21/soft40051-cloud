package com.ntu.cloudgui.app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnectionManager {

    // Read database connection details from environment variables
    private static final String HOST = System.getenv().getOrDefault("DB_HOST", "lamp-server");
    private static final int    PORT = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "3306"));
    private static final String DB   = System.getenv().getOrDefault("DB_NAME", "dbtutorial");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASS = System.getenv("DB_PASS");

    private static final String URL = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
            HOST, PORT, DB
    );

    public static Connection getConnection() throws SQLException {
        if (USER == null || PASS == null) {
            throw new SQLException("Database credentials (DB_USER and DB_PASS) are not set in the environment.");
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}