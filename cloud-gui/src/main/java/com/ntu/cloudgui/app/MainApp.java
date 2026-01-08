package com.ntu.cloudgui.app;

import com.ntu.cloudgui.app.db.DatabaseManager;
import com.ntu.cloudgui.app.service.SyncService;
import com.ntu.cloudgui.app.session.SessionState;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the JavaFX GUI application.
 */
public class MainApp extends Application {

    private SyncService syncService;
    private Thread syncThread;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Initialize databases and connectivity status
        DatabaseManager.initializeDatabases();

        // 2. Start the background synchronization service
        syncService = new SyncService();
        syncThread = new Thread(syncService);
        syncThread.setDaemon(true); // Allows the app to exit even if this thread is running
        syncThread.start();
        System.out.println("SyncService started.");

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

        // Existing in-memory default admin
        SessionState.getInstance().ensureDefaultAdmin();
    }

    @Override
    public void stop() throws Exception {
        // 1. Stop the synchronization service
        if (syncService != null) {
            syncService.stop();
        }
        if (syncThread != null) {
            syncThread.interrupt(); // Interrupt the sleep to allow for a quick shutdown
        }
        System.out.println("SyncService stopped.");

        // 2. Close the MySQL database connection
        DatabaseManager.closeMySqlConnection();

        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
