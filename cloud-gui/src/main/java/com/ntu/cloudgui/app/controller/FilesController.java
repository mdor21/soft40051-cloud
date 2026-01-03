package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.api.LoadBalancerClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Files Controller - File Upload/Download UI
 * 
 * CONNECTIVITY FLOW:
 * 1. User selects file → Read file data from disk
 * 2. Create LoadBalancerClient → Send file to Load Balancer (TCP/IP port 8080)
 * 3. Load Balancer → Routes to Aggregator based on scheduling algorithm
 * 4. Aggregator → Chunks file, calculates CRC32, distributes to File Servers via SSH
 * 5. Aggregator → Stores metadata in MySQL
 * 6. GUI SQLite → Syncs with MySQL for offline capability
 * 
 * On Download:
 * 1. GUI → Load Balancer (TCP/IP) → Aggregator
 * 2. Aggregator → Retrieves chunks from File Servers (SSH)
 * 3. Aggregator → Reassembles, verifies CRC32
 * 4. GUI → Saves to local disk
 */
public class FilesController {
    
    @FXML private ListView<String> filesList;
    @FXML private TextArea contentArea;
    @FXML private TextField fileNameField;
    
    private LoadBalancerClient loadBalancerClient;
    
    @FXML
    public void initialize() {
        loadBalancerClient = new LoadBalancerClient();
        loadFileList();
    }
    
    @FXML
    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to upload");
        File file = fileChooser.showOpenDialog(null);
        
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                loadBalancerClient.uploadFile(
                    file.getName(),
                    file.length(),
                    fileData
                )
                .thenAccept(response -> {
                    showInfo("✓ Uploaded: " + file.getName() + " (ID: " + response.fileId + ")");
                    loadFileList();
                })
                .exceptionally(ex -> {
                    showAlert("Upload error: " + ex.getMessage());
                    return null;
                });
            } catch (IOException e) {
                showAlert("Upload error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleDownload() {
        String selected = filesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Select a file to download");
            return;
        }
        
        loadBalancerClient.downloadFile(selected)
            .thenAccept(response -> {
                try {
                    Path downloadPath = Path.of(System.getProperty("user.home"), "Downloads", selected);
                    Files.write(downloadPath, response.fileData);
                    showInfo("✓ Downloaded: " + selected);
                    loadFileList();
                } catch (IOException e) {
                    showAlert("Save error: " + e.getMessage());
                }
            })
            .exceptionally(ex -> {
                showAlert("Download error: " + ex.getMessage());
                return null;
            });
    }
    
    private void loadFileList() {
        filesList.getItems().clear();
        try {
            Path downloadsPath = Path.of(System.getProperty("user.home"), "Downloads");
            if (Files.exists(downloadsPath)) {
                Files.list(downloadsPath)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .forEach(f -> filesList.getItems().add(f));
            }
        } catch (IOException e) {
            showAlert("Error loading files: " + e.getMessage());
        }
        
        filesList.setOnMouseClicked(event -> {
            String selected = filesList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    Path path = Path.of(System.getProperty("user.home"), "Downloads", selected);
                    String content = Files.readString(path);
                    contentArea.setText(content);
                } catch (IOException e) {
                    contentArea.setText("[Binary file or read error]");
                }
            }
        });
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Alert");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
