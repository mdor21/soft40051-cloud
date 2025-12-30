package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.service.LocalTerminalService;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class LocalTerminalController {

    @FXML private TextArea outputArea;
    @FXML private TextField commandField;

    private final LocalTerminalService service = new LocalTerminalService();

    @FXML
    private void handleRun() {
        String cmd = commandField.getText();
        System.out.println("handleRun called with: " + cmd);
        outputArea.appendText("$ " + cmd + "\n");
        try {
            String result = service.execute(cmd);
            outputArea.appendText(result + "\n");
        } catch (Exception e) {
            outputArea.appendText("Error: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
        commandField.clear();
    }
}
