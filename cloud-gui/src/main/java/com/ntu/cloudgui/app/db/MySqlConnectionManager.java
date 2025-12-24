package com.ntu.cloudgui.app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnectionManager {

    // Read database connection details from environment variables
    private static final String HOST = System.getenv().getOrDefault("DB_HOST", "lamp-server");
    private static final int    PORT = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "3306"));
    private static final String DB   = System.getenv().getOrDefault("DB_NAME", "cloud_gui");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "admin");
    private static final String PASS = System.getenv().getOrDefault("DB_PASS", "changeme");

    private static final String URL = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
            HOST, PORT, DB
    );

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
