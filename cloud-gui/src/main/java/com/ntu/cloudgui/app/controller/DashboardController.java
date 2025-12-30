package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.session.SessionState;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private Button usersButton;
    @FXML private Button logsButton;
    @FXML private StackPane contentPane;

    @FXML
    private void initialize() {
        userLabel.setText("User: " + SessionState.getInstance().getCurrentUser().getUsername());
        statusLabel.setText(SessionState.getInstance().isOnline() ? "Online" : "Offline");

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
}
