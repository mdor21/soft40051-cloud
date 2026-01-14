package com.ntu.cloudgui.app.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class LocalDatabaseInitializer {

    private static final String DB_DIR = System.getProperty("user.home") + "/.local/share/comp20081";
    private static final String DB_PATH = DB_DIR + "/comp20081.db";

    public static Connection init() throws SQLException, IOException {
        ensureDatabaseDirectory();
        Connection conn = DriverManager.getConnection(getJdbcUrl());
        createSchema(conn);
        return conn;
    }

    public static String getJdbcUrl() {
        return "jdbc:sqlite:" + DB_PATH;
    }

    public static void ensureDatabaseDirectory() throws IOException {
        Path dir = Paths.get(DB_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private static void createSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS session_users");
            stmt.execute("DROP TABLE IF EXISTS file_cache");
            stmt.execute("DROP TABLE IF EXISTS sync_queue");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Session_Cache (" +
                "    session_id TEXT PRIMARY KEY," +
                "    user_id INTEGER NOT NULL," +
                "    username TEXT NOT NULL," +
                "    role TEXT NOT NULL," +
                "    login_time INTEGER NOT NULL," +
                "    last_sync INTEGER DEFAULT 0" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Local_File_Metadata (" +
                "    file_id TEXT PRIMARY KEY," +
                "    owner_id INTEGER NOT NULL," +
                "    original_filename TEXT NOT NULL," +
                "    file_size INTEGER NOT NULL," +
                "    total_chunks INTEGER NOT NULL," +
                "    sync_status TEXT DEFAULT 'pending'," +
                "    last_modified INTEGER NOT NULL," +
                "    last_sync INTEGER DEFAULT 0" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Pending_Operations (" +
                "    operation_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    operation_type TEXT NOT NULL," +
                "    file_id TEXT," +
                "    payload TEXT," +
                "    created_at INTEGER NOT NULL," +
                "    retry_count INTEGER DEFAULT 0" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Local_Logs (" +
                "    log_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    timestamp INTEGER NOT NULL," +
                "    event_type TEXT NOT NULL," +
                "    user_id INTEGER," +
                "    description TEXT NOT NULL," +
                "    severity TEXT DEFAULT 'INFO'," +
                "    synced INTEGER DEFAULT 0" +
                ")"
            );
        }
    }
}
