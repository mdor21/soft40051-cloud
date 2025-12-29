package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.model.LogEntry;
import com.ntu.cloudgui.app.service.LoggingService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class LogsController {

    @FXML private TableView<LogEntry> logsTable;
    @FXML private TableColumn<LogEntry, String> timeCol;
    @FXML private TableColumn<LogEntry, String> userCol;
    @FXML private TableColumn<LogEntry, String> actionCol;
    @FXML private TableColumn<LogEntry, String> detailsCol;
    @FXML private TableColumn<LogEntry, String> successCol;

    @FXML
    private void initialize() {
        timeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getTimestamp().toString()));
        userCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getUsername()));
        actionCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getAction()));
        detailsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDetails()));
        successCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Boolean.toString(c.getValue().isSuccess())));
        logsTable.setItems(FXCollections.observableArrayList(
                LoggingService.getInstance().getEntries()));
    }
}
