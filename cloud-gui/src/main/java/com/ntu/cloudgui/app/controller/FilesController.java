package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.api.LoadBalancerClient;
import com.ntu.cloudgui.app.session.SessionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.util.Pair;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javafx.fxml.FXMLLoader;

public class FilesController {

    @FXML private ListView<String> filesList;
    @FXML private TextArea contentArea;
    @FXML private TextField fileNameField;

    private LoadBalancerClient loadBalancerClient;

    @FXML
    public void initialize() {
        loadBalancerClient = new LoadBalancerClient();
        setupFileListListener();
        loadRemoteFileList();
    }

    private String getCurrentUsername() {
        return SessionState.getInstance().getCurrentUser().getUsername();
    }

    private void setupFileListListener() {
        filesList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fileNameField.setText(newSelection);
                // On selection, download the file content to the text area
                loadBalancerClient.downloadFile(newSelection)
                    .thenAccept(response -> {
                        if ("OK".equals(response.status) && response.fileData != null) {
                            Platform.runLater(() -> contentArea.setText(new String(response.fileData, StandardCharsets.UTF_8)));
                        } else {
                            Platform.runLater(() -> {
                                contentArea.setText("");
                                showAlert("Could not load file content: " + response.message);
                            });
                        }
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showAlert("Error downloading file: " + ex.getMessage()));
                        return null;
                    });
            }
        });
    }

    @FXML
    private void handleCreate() {
        String filename = fileNameField.getText();
        if (filename == null || filename.trim().isEmpty()) {
            showAlert("File name cannot be empty.");
            return;
        }
        loadBalancerClient.createFile(filename, getCurrentUsername())
            .thenAccept(response -> Platform.runLater(() -> {
                showInfo("File created: " + filename);
                loadRemoteFileList();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> showAlert("Error creating file: " + ex.getMessage()));
                return null;
            });
    }

    @FXML
    private void handleSave() {
        String selectedFile = filesList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showAlert("Please select a file to save.");
            return;
        }
        String content = contentArea.getText();
        byte[] fileData = content.getBytes(StandardCharsets.UTF_8);

        loadBalancerClient.updateFile(selectedFile, fileData.length, fileData)
            .thenAccept(response -> Platform.runLater(() -> {
                showInfo("✓ Saved: " + selectedFile);
                loadRemoteFileList(); // Reload to show updated size/date
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> showAlert("Save error: " + ex.getMessage()));
                return null;
            });
    }

    @FXML
    private void handleDelete() {
        String selectedFile = filesList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showAlert("Please select a file to delete.");
            return;
        }
        loadBalancerClient.deleteFile(selectedFile, getCurrentUsername())
            .thenAccept(response -> Platform.runLater(() -> {
                showInfo("File deleted: " + selectedFile);
                contentArea.clear();
                fileNameField.clear();
                loadRemoteFileList();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> showAlert("Delete error: " + ex.getMessage()));
                return null;
            });
    }

    @FXML
    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                loadBalancerClient.uploadFile(file.getName(), file.length(), fileData)
                    .thenAccept(response -> Platform.runLater(() -> {
                        showInfo("✓ Uploaded: " + file.getName());
                        loadRemoteFileList();
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showAlert("Upload error: " + ex.getMessage()));
                        return null;
                    });
            } catch (IOException e) {
                showAlert("File read error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDownload() {
        String selectedFile = filesList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showAlert("Select a file to download.");
            return;
        }
        loadBalancerClient.downloadFile(selectedFile)
            .thenAccept(response -> {
                try {
                    Path downloadPath = Path.of(System.getProperty("user.home"), "Downloads", selectedFile);
                    Files.createDirectories(downloadPath.getParent());
                    Files.write(downloadPath, response.fileData);
                    Platform.runLater(() -> showInfo("✓ Downloaded to: " + downloadPath));
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert("Save error: " + e.getMessage()));
                }
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> showAlert("Download error: " + ex.getMessage()));
                return null;
            });
    }

    private void loadRemoteFileList() {
        loadBalancerClient.listFiles(getCurrentUsername())
            .thenAccept(response -> {
                if ("OK".equals(response.status) && response.files != null) {
                    Platform.runLater(() -> {
                        filesList.getItems().clear();
                        for (LoadBalancerClient.FileMetadata file : response.files) {
                            filesList.getItems().add(file.filename);
                        }
                    });
                } else {
                    Platform.runLater(() -> showAlert("Could not load file list: " + response.message));
                }
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> showAlert("Error loading remote files: " + ex.getMessage()));
                return null;
            });
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

    @FXML
    private void handleShare() {
        String selectedFile = filesList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showAlert("Please select a file to share.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ntu/cloudgui/app/view/ShareDialog.fxml"));
            DialogPane dialogPane = loader.load();
            ShareDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Share File");

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String targetUsername = controller.getUsername();
                String permission = controller.getPermission();
                String ownerUsername = getCurrentUsername();

                if (targetUsername.isEmpty()) {
                    showAlert("Target username cannot be empty.");
                    return;
                }

                loadBalancerClient.shareFile(selectedFile, ownerUsername, targetUsername, permission)
                    .thenAccept(response -> Platform.runLater(() -> {
                        if ("OK".equals(response.status)) {
                            showInfo("File shared successfully with " + targetUsername);
                        } else {
                            showAlert("Sharing failed: " + response.message);
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showAlert("Sharing error: " + ex.getMessage()));
                        return null;
                    });
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not open share dialog.");
        }
    }
}
