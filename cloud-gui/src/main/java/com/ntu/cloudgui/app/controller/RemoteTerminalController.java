package com.ntu.cloudgui.app.controller;

import com.jcraft.jsch.JSchException;
import com.ntu.cloudgui.app.service.RemoteTerminalService;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RemoteTerminalController {

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField userField;
    @FXML private PasswordField passwordField;
    @FXML private TextArea remoteOutputArea;
    @FXML private TextField remoteCommandField;

    private final RemoteTerminalService service = new RemoteTerminalService();

    @FXML
    private void handleRun() {
        try {
            String host = hostField.getText();
            int port = Integer.parseInt(portField.getText());
            String user = userField.getText();
            String pass = passwordField.getText();
            String cmd = remoteCommandField.getText();

            String output = service.executeOnHost(host, port, user, pass, cmd);
            remoteOutputArea.appendText(user + "@" + host + " $ " + cmd + "\n" + output + "\n");
        } catch (NumberFormatException | JSchException e) {
            remoteOutputArea.appendText("Error: " + e.getMessage() + "\n");
        }
        remoteCommandField.clear();
    }
}
