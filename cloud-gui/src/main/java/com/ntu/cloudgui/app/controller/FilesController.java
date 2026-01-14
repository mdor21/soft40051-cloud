package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.service.FileService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
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

    private FileService fileService;

    @FXML
    public void initialize() {
        fileService = new FileService();
        setupFileListListener();
        loadRemoteFileList();
    }

    private void setupFileListListener() {
        filesList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fileNameField.setText(newSelection);
                // On selection, download the file content to the text area
                fileService.downloadFile(newSelection)
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
        fileService.createFile(filename)
            .thenAccept(result -> Platform.runLater(() -> {
                if (result.success) {
                    showInfo(result.message);
                    loadRemoteFileList();
                } else {
                    showAlert(result.message);
                }
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

        fileService.saveFile(selectedFile, fileData)
            .thenAccept(result -> Platform.runLater(() -> {
                if (result.success) {
                    showInfo("✓ " + result.message);
                    loadRemoteFileList(); // Reload to show updated size/date
                } else {
                    showAlert(result.message);
                }
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
        fileService.deleteFile(selectedFile)
            .thenAccept(result -> Platform.runLater(() -> {
                if (result.success) {
                    showInfo(result.message);
                    contentArea.clear();
                    fileNameField.clear();
                    loadRemoteFileList();
                } else {
                    showAlert(result.message);
                }
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
                fileService.uploadFile(file.getName(), fileData)
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result.success) {
                            showInfo("✓ " + result.message);
                            loadRemoteFileList();
                        } else {
                            showAlert(result.message);
                        }
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
        fileService.downloadFile(selectedFile)
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
        fileService.listFilesForCurrentUser()
            .thenAccept(files -> Platform.runLater(() -> {
                filesList.getItems().clear();
                filesList.getItems().addAll(files);
            }))
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
                if (targetUsername.isEmpty()) {
                    showAlert("Target username cannot be empty.");
                    return;
                }

                fileService.shareFile(selectedFile, targetUsername, permission)
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result.success) {
                            showInfo(result.message);
                        } else {
                            showAlert(result.message);
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
