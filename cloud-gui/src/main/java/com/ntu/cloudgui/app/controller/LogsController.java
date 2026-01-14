package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.model.LogEntry;
import com.ntu.cloudgui.app.db.DatabaseManager;
import com.ntu.cloudgui.app.db.SystemLogRepository;
import com.ntu.cloudgui.app.service.LoggingService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LogsController {

    @FXML private TableView<LogEntry> logsTable;
    @FXML private TableColumn<LogEntry, String> timeCol;
    @FXML private TableColumn<LogEntry, String> userCol;
    @FXML private TableColumn<LogEntry, String> actionCol;
    @FXML private TableColumn<LogEntry, String> detailsCol;
    @FXML private TableColumn<LogEntry, String> successCol;

    private final SystemLogRepository systemLogRepository = new SystemLogRepository();

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
        refreshLogs();
    }

    private void refreshLogs() {
        List<LogEntry> merged = new ArrayList<>();
        if (DatabaseManager.isMysqlConnected()) {
            try {
                merged.addAll(systemLogRepository.fetchRecent(200));
            } catch (Exception e) {
                System.err.println("Failed to load System_Logs: " + e.getMessage());
            }
        }
        merged.addAll(LoggingService.getInstance().getEntries());
        merged.sort(Comparator.comparing(LogEntry::getTimestamp).reversed());
        logsTable.setItems(FXCollections.observableArrayList(merged));
    }
}
