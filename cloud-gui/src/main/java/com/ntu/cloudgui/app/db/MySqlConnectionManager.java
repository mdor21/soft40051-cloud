package com.ntu.cloudgui.app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnectionManager {

    // MySQL is exposed on the host at 3306 and forwarded into the VM
    // In the lab image this is usually reachable as 127.0.0.1:3306
    private static final String HOST = "127.0.0.1";      // NOT "lamp-server" inside the VM
    private static final int    PORT = 3306;
    private static final String DB   = "cloud_gui";      // your schema name
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
