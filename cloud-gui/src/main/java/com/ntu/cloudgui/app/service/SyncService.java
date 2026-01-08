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

import java.util.List;

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
            System.out.println("SyncService: Sync queue is empty.");
            return;
        }

        System.out.println("SyncService: Found " + operations.size() + " operations to sync.");

        for (SessionCacheRepository.SyncOperation op : operations) {
            try {
                handleOperation(op);
                sessionCacheRepository.deleteQueuedOperation(op.id);
                System.out.println("Successfully synced and deleted operation ID: " + op.id);
            } catch (Exception e) {
                System.err.println("SyncService: Failed to sync operation " + op.id + ". Error: " + e.getMessage());
            }
        }
    }

    private void handleOperation(SessionCacheRepository.SyncOperation op) throws Exception {
        switch (op.entityType) {
            case "USER":
                handleUserOperation(op);
                break;
            case "FILE_METADATA":
                handleFileMetadataOperation(op);
                break;
            case "ACL":
                handleAclOperation(op);
                break;
            case "SYSTEM_LOG":
                handleSystemLogOperation(op);
                break;
            default:
                System.err.println("SyncService: Unknown entity type: " + op.entityType);
        }
    }

    private void handleUserOperation(SessionCacheRepository.SyncOperation op) throws Exception {
        User user = gson.fromJson(op.payload, User.class);
        switch (op.operation) {
            case "CREATE":
            case "UPDATE":
                userRepository.save(user);
                break;
            case "DELETE":
                userRepository.deleteByUsername(user.getUsername());
                break;
            default:
                 System.err.println("SyncService: Unknown user operation: " + op.operation);
        }
    }

    private void handleFileMetadataOperation(SessionCacheRepository.SyncOperation op) throws Exception {
        FileMetadata fileMetadata = gson.fromJson(op.payload, FileMetadata.class);
        switch (op.operation) {
            case "CREATE":
                fileMetadataRepository.save(fileMetadata.getId(), fileMetadata.getName(), fileMetadata.getOwner(), fileMetadata.getSizeBytes());
                break;
            case "DELETE":
                fileMetadataRepository.delete(fileMetadata.getId());
                break;
            default:
                 System.err.println("SyncService: Unknown file metadata operation: " + op.operation);
        }
    }

    private void handleAclOperation(SessionCacheRepository.SyncOperation op) throws Exception {
        Acl acl = gson.fromJson(op.payload, Acl.class);
        switch (op.operation) {
            case "CREATE":
                aclRepository.grantPermission(acl.getFileId(), acl.getUsername(), acl.getPermission());
                break;
            case "DELETE":
                aclRepository.revokePermission(acl.getFileId(), acl.getUsername());
                break;
            default:
                 System.err.println("SyncService: Unknown ACL operation: " + op.operation);
        }
    }

    private void handleSystemLogOperation(SessionCacheRepository.SyncOperation op) throws Exception {
        SystemLog log = gson.fromJson(op.payload, SystemLog.class);
        systemLogRepository.logEvent(log.getEventType(), log.getUserId(), log.getDescription());
    }
}
