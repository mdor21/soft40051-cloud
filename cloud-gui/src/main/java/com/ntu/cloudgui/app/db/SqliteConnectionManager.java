package com.ntu.cloudgui.app.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqliteConnectionManager {

    private static final String URL = LocalDatabaseInitializer.getJdbcUrl();

    public static Connection getConnection() throws SQLException {
        try {
            LocalDatabaseInitializer.ensureDatabaseDirectory();
        } catch (IOException e) {
            throw new SQLException("Failed to create SQLite data directory", e);
        }
        return DriverManager.getConnection(URL);
    }
}
