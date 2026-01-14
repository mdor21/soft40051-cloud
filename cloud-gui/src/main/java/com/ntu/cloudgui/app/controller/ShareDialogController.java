package com.ntu.cloudgui.app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

public class ShareDialogController {

    @FXML private TextField usernameField;
    @FXML private ToggleGroup permissionGroup;
    @FXML private RadioButton readPermissionRadio;
    @FXML private RadioButton writePermissionRadio;

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPermission() {
        RadioButton selected = (RadioButton) permissionGroup.getSelectedToggle();
        if (selected == null) {
            return "READ"; // Default to read-only
        }
        return "READ/WRITE".equalsIgnoreCase(selected.getText()) ? "WRITE" : "READ";
    }
}
