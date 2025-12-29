package com.ntu.cloudgui.app.controller;

import com.ntu.cloudgui.app.client.LoadBalancerClient;
import com.ntu.cloudgui.app.model.FileMeta;
import com.ntu.cloudgui.app.service.FileService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Collection;

public class FilesController {

    @FXML private ListView<FileMeta> filesList;
    @FXML private TextField fileNameField;
    @FXML private TextArea contentArea;

    private final FileService fileService = new FileService();

    @FXML
    private void initialize() {
        filesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FileMeta item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        filesList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> onFileSelected(newVal));

        refreshList();
    }

    private void refreshList() {
        Collection<FileMeta> files = fileService.listFilesForCurrentUser();
        filesList.setItems(FXCollections.observableArrayList(files));
    }

    private void onFileSelected(FileMeta meta) {
        if (meta == null) {
            fileNameField.clear();
            contentArea.clear();
            return;
        }

        fileNameField.setText(meta.getName());
        // Optional: load a preview from backend if supported
        contentArea.setText("");
    }

    @FXML
    private void handleCreate() {
        String name = fileNameField.getText();
        String content = contentArea.getText();

        if (name == null || name.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "File name is required.");
            return;
        }

        FileMeta meta = fileService.createFile(name, content);
        refreshList();
        filesList.getSelectionModel().select(meta);
    }

    @FXML
    private void handleSave() {
        FileMeta meta = filesList.getSelectionModel().getSelectedItem();
        if (meta == null) {
            showAlert(Alert.AlertType.WARNING, "Select a file to save.");
            return;
        }

        String content = contentArea.getText();
        fileService.updateFile(meta.getId(), content);
    }

    @FXML
    private void handleDelete() {
        FileMeta meta = filesList.getSelectionModel().getSelectedItem();
        if (meta == null) {
            showAlert(Alert.AlertType.WARNING, "Select a file to delete.");
            return;
        }

        fileService.deleteFile(meta.getId());
        refreshList();
        fileNameField.clear();
        contentArea.clear();
    }

    @FXML
    private void handleShare() {
        showAlert(Alert.AlertType.INFORMATION,
                "File sharing / ACL UI placeholder (DB integration later).");
    }

    @FXML
    private void handleUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload File");
        File chosen = chooser.showOpenDialog(filesList.getScene().getWindow());
        if (chosen == null) {
            return;
        }

        try {
            long fileSize = chosen.length();

            // Send file to Load Balancer
            try (InputStream fis = new FileInputStream(chosen)) {
                String fileId = LoadBalancerClient.uploadFile(fis, chosen.getName(), fileSize);

                // Store metadata locally, including remote file id
                FileMeta meta = fileService.createFile(chosen.getName(), "");
                meta.setRemoteFileId(fileId);
                fileService.updateFile(meta.getId(), "");

                refreshList();
                showAlert(Alert.AlertType.INFORMATION,
                        "File uploaded successfully. File ID: " + fileId);
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Upload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDownload() {
        FileMeta meta = filesList.getSelectionModel().getSelectedItem();
        if (meta == null) {
            showAlert(Alert.AlertType.WARNING, "Select a file to download.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose download folder");
        File dir = chooser.showDialog(filesList.getScene().getWindow());
        if (dir == null) {
            return;
        }

        String remoteFileId = meta.getRemoteFileId();
        if (remoteFileId == null || remoteFileId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "File has no remote ID. Cannot download.");
            return;
        }

        try {
            File outFile = new File(dir, meta.getName());
            try (InputStream in = LoadBalancerClient.downloadFile(remoteFileId);
                 OutputStream fos = new FileOutputStream(outFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            showAlert(Alert.AlertType.INFORMATION,
                    "File downloaded successfully to: " + outFile.getAbsolutePath());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Download failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.showAndWait();
    }
}
