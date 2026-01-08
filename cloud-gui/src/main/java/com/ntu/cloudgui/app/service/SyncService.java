package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.db.DatabaseManager;
import com.ntu.cloudgui.app.db.SessionCacheRepository;
import com.ntu.cloudgui.app.db.UserRepository;
import com.ntu.cloudgui.app.db.FileMetadataRepository;
import com.ntu.cloudgui.app.db.AclRepository;
import com.ntu.cloudgui.app.db.SystemLogRepository;
import com.ntu.cloudgui.app.model.User;
import com.ntu.cloudgui.app.model.FileMetadata;
import com.ntu.cloudgui.app.model.Acl;
import com.ntu.cloudgui.app.model.SystemLog;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A background service that synchronizes offline data with the remote server.
 */
public class SyncService implements Runnable {

    private final SessionCacheRepository sessionCacheRepository;
    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final AclRepository aclRepository;
    private final SystemLogRepository systemLogRepository;
    private final Gson gson = new Gson();
    private volatile boolean running = true;

    public SyncService() {
        this.sessionCacheRepository = new SessionCacheRepository();
        this.userRepository = new UserRepository();
        this.fileMetadataRepository = new FileMetadataRepository();
        this.aclRepository = new AclRepository();
        this.systemLogRepository = new SystemLogRepository();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            if (DatabaseManager.isMysqlConnected()) {
                try {
                    System.out.println("SyncService: Online. Checking for queued operations...");
                    processSyncQueue();
                } catch (Exception e) {
                    System.err.println("SyncService: Error processing sync queue: " + e.getMessage());
                }
            } else {
                System.out.println("SyncService: Offline. Will check again later.");
            }

            try {
                // Wait for a minute before the next sync attempt
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void processSyncQueue() throws Exception {
        List<SessionCacheRepository.SyncOperation> operations = sessionCacheRepository.getQueuedOperations();
        if (operations.isEmpty()) {
            return;
        }

        for (SessionCacheRepository.SyncOperation op : operations) {
            try {
                boolean success = handleOperation(op);
                if (success) {
                    sessionCacheRepository.deleteQueuedOperation(op.id);
                    System.out.println("Successfully synced and deleted operation ID: " + op.id);
                }
            } catch (Exception e) {
                System.err.println("SyncService: Failed to sync operation " + op.id + ". Error: " + e.getMessage());
            }
        }
    }

    private boolean handleOperation(SessionCacheRepository.SyncOperation op) throws Exception {
        switch (op.entityType) {
            case "USER":
                return handleUserOperation(op);
            // Other cases...
        }
        return true;
    }

    private boolean handleUserOperation(SessionCacheRepository.SyncOperation op) throws Exception {
        User localUser = gson.fromJson(op.payload, User.class);
        User remoteUser = userRepository.findByUsername(localUser.getUsername());

        if (remoteUser != null && localUser.getLastModified() != null &&
            remoteUser.getLastModified().after(localUser.getLastModified())) {

            if (op.operation.equals("UPDATE") && !localUser.getRole().equals(remoteUser.getRole())) {
                logConflict("User role change conflict for " + localUser.getUsername(), localUser.getId());

                CompletableFuture<Boolean> future = new CompletableFuture<>();
                showConflictResolutionDialog(
                    "User role conflict",
                    "The role for user '" + localUser.getUsername() + "' was changed on the server. " +
                    "Local change: " + localUser.getRole() + ", Server change: " + remoteUser.getRole() + ". " +
                    "Do you want to overwrite the server's version?",
                    future
                );

                if (future.get()) { // Blocks until the user makes a choice
                    userRepository.save(localUser); // Force overwrite
                }
                return true; // Conflict is resolved
            } else {
                logConflict("User update conflict (last-write-wins) for " + localUser.getUsername(), localUser.getId());
                return true; // Discard local change
            }
        }

        switch (op.operation) {
            case "CREATE":
            case "UPDATE":
                userRepository.save(localUser);
                break;
            case "DELETE":
                userRepository.deleteByUsername(localUser.getUsername());
                break;
        }
        return true;
    }

    // Other handlers...

    private void logConflict(String description, Long userId) throws Exception {
        systemLogRepository.logEvent("Sync Conflict Resolved", userId.toString(), description);
    }

    private void showConflictResolutionDialog(String title, String message, CompletableFuture<Boolean> future) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            Optional<ButtonType> result = alert.showAndWait();
            future.complete(result.isPresent() && result.get() == ButtonType.OK);
        });
    }
}
