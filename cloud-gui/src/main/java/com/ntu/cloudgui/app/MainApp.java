package com.ntu.cloudgui.app;

import com.ntu.cloudgui.app.db.MySqlConnectionManager;
import com.ntu.cloudgui.app.db.SessionCacheRepository;
import com.ntu.cloudgui.app.session.SessionState;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Main entry point for the JavaFX GUI application.
 * 
 * CONNECTIVITY FLOW:
 * 1. Initializes local SQLite session cache for offline capability
 * 2. Tests remote MySQL connectivity (marks system as online/offline)
 * 3. Loads login UI from FXML
 * 4. Manages authentication and session state throughout application lifecycle
 * 
 * Connects to:
 * - MySQL Database (via MySqlConnectionManager) - port 3306 (internal Docker network)
 * - Local SQLite Database (offline session cache)
 * - Load Balancer (for file upload/download operations - to be integrated in FilesController)
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Initialize local SQLite session cache schema
        initLocalSessionCache();

        // 2. Test remote MySQL connectivity (for online/offline decision)
        testMySqlConnection();

        // 3. Load the login UI
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/ntu/cloudgui/app/view/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 960, 600);
        scene.getStylesheets().add(
                getClass().getResource("/com/ntu/cloudgui/app/css/modern.css")
                        .toExternalForm());

        primaryStage.setTitle("NTU File Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Existing in-memory default admin (you can later replace this with DB bootstrap)
        SessionState.getInstance().ensureDefaultAdmin();
    }

    /**
     * Initialize the local SQLite database schema for session caching.
     * This allows the application to function in offline mode.
     */
    private void initLocalSessionCache() {
        SessionCacheRepository repo = new SessionCacheRepository();
        try {
            repo.initSchema();
            System.out.println("SQLite session cache initialised.");
        } catch (SQLException e) {
            System.err.println("Failed to initialise SQLite session cache: " + e.getMessage());
        }
    }

    /**
     * Test connection to remote MySQL database.
     * If connection fails, application operates in offline mode with local SQLite only.
     * 
     * CONNECTIVITY: MySQL Container
     * - Host: mysql (service name in Docker network: soft40051_network)
     * - Port: 3306 (internal Docker port)
     * - Database: cloudgui_db
     */
    private void testMySqlConnection() {
        try (Connection conn = MySqlConnectionManager.getConnection()) {
            System.out.println("Connected to remote MySQL successfully.");
            SessionState.getInstance().setOnline(true);
        } catch (SQLException e) {
            System.err.println("MySQL unavailable, application may run in offline mode: " + e.getMessage());
            SessionState.getInstance().setOnline(false);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
