package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.service.SyncService;
import com.ntu.cloudgui.app.session.SessionState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class DashboardController {

    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private Button usersButton;
    @FXML private Button logsButton;
    @FXML private StackPane contentPane;
    private Timeline statusTimer;

    @FXML
    private void initialize() {
        userLabel.setText("User: " + SessionState.getInstance().getCurrentUser().getUsername());
        updateStatusLabel();
        startStatusTimer();

        boolean isAdmin = SessionState.getInstance().isAdmin();
        usersButton.setVisible(isAdmin);
        logsButton.setVisible(isAdmin);

        showFiles();
    }

    private void setContent(String fxml) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxml));
            contentPane.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showFiles() {
        setContent("/com/ntu/cloudgui/app/view/files.fxml");
    }

    @FXML
    private void showLocalTerminal() {
        setContent("/com/ntu/cloudgui/app/view/local_terminal.fxml");
    }

    @FXML
    private void showRemoteTerminal() {
        setContent("/com/ntu/cloudgui/app/view/remote_terminal.fxml");
    }

    @FXML
    private void showUsers() {
        setContent("/com/ntu/cloudgui/app/view/users.fxml");
    }

    @FXML
    private void showLogs() {
        setContent("/com/ntu/cloudgui/app/view/logs.fxml");
    }

    @FXML
    private void handleSyncNow() {
        SyncService syncService = SyncService.getInstance();
        if (syncService == null) {
            showAlert("Sync service is not running yet.");
            return;
        }
        syncService.triggerManualSync()
            .thenAccept(success -> javafx.application.Platform.runLater(() -> {
                updateStatusLabel();
                if (success) {
                    showInfo("Sync completed.");
                } else {
                    showAlert("Sync failed or offline.");
                }
            }));
    }

    @FXML
    private void handleLogout() {
        Stage stage = (Stage) contentPane.getScene().getWindow();
        try {
            javafx.fxml.FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ntu/cloudgui/app/view/login.fxml"));
            stage.setScene(new javafx.scene.Scene(loader.load(), 960, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startStatusTimer() {
        statusTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> updateStatusLabel()));
        statusTimer.setCycleCount(Timeline.INDEFINITE);
        statusTimer.play();
    }

    private void updateStatusLabel() {
        boolean online = SessionState.getInstance().isOnline();
        statusLabel.setText(online ? "Online" : "Offline");
        statusLabel.getStyleClass().removeAll("status-online", "status-offline");
        statusLabel.getStyleClass().add(online ? "status-online" : "status-offline");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Alert");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
