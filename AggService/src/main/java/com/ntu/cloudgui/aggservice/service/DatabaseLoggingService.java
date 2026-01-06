package com.ntu.cloudgui.aggservice.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseLoggingService {

    private final Connection connection;

    public DatabaseLoggingService(Connection connection) {
        this.connection = connection;
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void warn(String message) {
        log("WARN", message);
    }

    private void log(String level, String message) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO logs (level, message) VALUES (?, ?)")) {
            statement.setString(1, level);
            statement.setString(2, message);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
