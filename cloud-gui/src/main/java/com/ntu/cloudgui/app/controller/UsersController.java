package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.model.Role;
import com.ntu.cloudgui.app.model.User;
import com.ntu.cloudgui.app.session.SessionState;
import com.ntu.cloudgui.app.service.AuthService;
import com.ntu.cloudgui.app.db.UserRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TextField newUserField;
    @FXML private PasswordField newPasswordField;
    @FXML private ChoiceBox<Role> roleChoice;

    private final AuthService authService = new AuthService();
    private final UserRepository userRepo = new UserRepository();

    @FXML
    private void initialize() {
        if (!SessionState.getInstance().isAdmin()) {
            throw new IllegalStateException("Users view should only be visible to admins");
        }
        usernameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getUsername()));
        roleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getRole().name()));

        roleChoice.setItems(FXCollections.observableArrayList(Role.STANDARD, Role.ADMIN));
        roleChoice.setValue(Role.STANDARD);

        refresh();
    }

    private void refresh() {
        try {
            usersTable.setItems(FXCollections.observableArrayList(userRepo.findAll()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateUser() {
        authService.createUser(newUserField.getText(),
                newPasswordField.getText(),
                roleChoice.getValue());
        newUserField.clear();
        newPasswordField.clear();
        refresh();
    }

    @FXML
    private void handlePromote() {
        User u = usersTable.getSelectionModel().getSelectedItem();
        if (u != null) {
            authService.changeRole(u.getUsername(), Role.ADMIN);
            refresh();
        }
    }

    @FXML
    private void handleDemote() {
        User u = usersTable.getSelectionModel().getSelectedItem();
        if (u != null) {
            authService.changeRole(u.getUsername(), Role.STANDARD);
            refresh();
        }
    }

    @FXML
    private void handleDelete() {
        User u = usersTable.getSelectionModel().getSelectedItem();
        if (u != null) {
            authService.deleteUser(u.getUsername());
            refresh();
        }
    }
}
