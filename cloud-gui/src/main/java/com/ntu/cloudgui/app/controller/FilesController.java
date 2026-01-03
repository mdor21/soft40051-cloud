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
    
    @FXML private ListView<String> fileListView;
    @FXML private Button uploadButton;
    @FXML private Button downloadButton;
    @FXML private Label statusLabel;
    @FXML private ProgressBar uploadProgress;
    
    private LoadBalancerClient loadBalancerClient;
    
    /**
     * Initialize controller
     */
    @FXML
    public void initialize() {
        loadBalancerClient = new LoadBalancerClient();
        updateStatus("Ready");
        loadFileList();
    }
    
    /**
     * Handle file upload
     */
    @FXML
    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file to upload");
        File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile == null) return;
        
        try {
            updateStatus("Reading file: " + selectedFile.getName());
            byte[] fileData = Files.readAllBytes(selectedFile.toPath());
            
            updateStatus("Uploading to Load Balancer...");
            loadBalancerClient.uploadFile(selectedFile.getName(), fileData.length, fileData)
                .thenAccept(response -> {
                    if ("SUCCESS".equals(response.status)) {
                        updateStatus("✓ Upload complete: " + selectedFile.getName());
                        loadFileList();
                    } else {
                        updateStatus("✗ Upload failed: " + response.message);
                    }
                })
                .exceptionally(ex -> {
                    updateStatus("✗ Upload error: " + ex.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            updateStatus("✗ Error reading file: " + e.getMessage());
        }
    }
    
    /**
     * Handle file download
     */
    @FXML
    private void handleDownload() {
        String selectedFile = fileListView.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            updateStatus("Select a file first");
            return;
        }
        
        try {
            updateStatus("Downloading: " + selectedFile);
            loadBalancerClient.downloadFile(selectedFile)
                .thenAccept(response -> {
                    if ("SUCCESS".equals(response.status)) {
                        try {
                            // Save file to disk
                            Path path = Path.of(System.getProperty("user.home"), "Downloads", selectedFile);
                            Files.write(path, response.fileData);
                            updateStatus("✓ Downloaded: " + selectedFile);
                        } catch (IOException e) {
                            updateStatus("✗ File write error: " + e.getMessage());
                        }
                    } else {
                        updateStatus("✗ Download failed: " + response.message);
                    }
                })
                .exceptionally(ex -> {
                    updateStatus("✗ Download error: " + ex.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            updateStatus("✗ Error: " + e.getMessage());
        }
    }
    
    /**
     * Reload file list from server
     */
    private void loadFileList() {
        loadBalancerClient.getStatus()
            .thenAccept(status -> {
                // TODO: Fetch actual file list from server
                updateStatus("Active nodes: " + status.activeNodes + "/" + status.totalNodes);
            })
            .exceptionally(ex -> {
                updateStatus("⚠ Offline mode - Local SQLite only");
                return null;
            });
    }
    
    /**
     * Update status label
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
}
