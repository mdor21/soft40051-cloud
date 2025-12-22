package com.ntu.cloudgui.app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnectionManager {

    // MySQL container name reachable on the shared Docker network
    private static final String HOST = "lamp-server";  // Docker service/container name
    private static final int    PORT = 3306;
    private static final String DB   = "cloud_gui";    // your schema name
    private static final String USER = "admin";
    private static final String PASS = "om2YVGGmQAnE";

    private static final String URL = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
            HOST, PORT, DB
    );

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
