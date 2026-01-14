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
import com.ntu.cloudgui.app.session.SessionState;
import com.ntu.cloudgui.app.client.LoadBalancerClient;
import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A background service that synchronizes offline data with the remote server.
 */
public class SyncService implements Runnable {

    private static volatile SyncService INSTANCE;
    private static final long SYNC_INTERVAL_MS = 60_000L;
    private static final long OFFLINE_RETRY_MIN_MS = 30_000L;
    private static final long OFFLINE_RETRY_MAX_MS = 5 * 60_000L;

    private final SessionCacheRepository sessionCacheRepository;
    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final AclRepository aclRepository;
    private final SystemLogRepository systemLogRepository;
    private final Gson gson = new Gson();
    private volatile boolean running = true;
    private long offlineDelayMs = OFFLINE_RETRY_MIN_MS;

    public SyncService() {
        INSTANCE = this;
        this.sessionCacheRepository = new SessionCacheRepository();
        this.userRepository = new UserRepository();
        this.fileMetadataRepository = new FileMetadataRepository();
        this.aclRepository = new AclRepository();
        this.systemLogRepository = new SystemLogRepository();
    }

    public static SyncService getInstance() {
        return INSTANCE;
    }

    public void stop() {
        running = false;
    }

    public CompletableFuture<Boolean> triggerManualSync() {
        return CompletableFuture.supplyAsync(() -> {
            if (!DatabaseManager.isMysqlConnected()) {
                SessionState.getInstance().setOnline(false);
                return false;
            }
            try {
                SessionState.getInstance().setOnline(true);
                syncOnce();
                return true;
            } catch (Exception e) {
                System.err.println("SyncService: Manual sync failed: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public void run() {
        while (running) {
            if (DatabaseManager.isMysqlConnected()) {
                try {
                    SessionState.getInstance().setOnline(true);
                    offlineDelayMs = OFFLINE_RETRY_MIN_MS;
                    System.out.println("SyncService: Online. Checking for queued operations...");
                    syncOnce();
                } catch (Exception e) {
                    System.err.println("SyncService: Error processing sync queue: " + e.getMessage());
                }

                sleep(SYNC_INTERVAL_MS);
            } else {
                SessionState.getInstance().setOnline(false);
                System.out.println("SyncService: Offline. Will check again later.");
                sleep(offlineDelayMs);
                offlineDelayMs = Math.min(OFFLINE_RETRY_MAX_MS, offlineDelayMs * 2);
            }
        }
    }

    private void syncOnce() throws Exception {
        pushPendingOperations();
        pushLocalLogs();
        pullRemoteMetadata();
    }

    private void pushPendingOperations() throws Exception {
        List<SessionCacheRepository.PendingOperation> operations = sessionCacheRepository.getPendingOperations();
        for (SessionCacheRepository.PendingOperation op : operations) {
            try {
                if (handlePendingOperation(op)) {
                    sessionCacheRepository.deletePendingOperation(op.id);
                } else {
                    sessionCacheRepository.incrementRetryCount(op.id, op.retryCount);
                }
            } catch (Exception e) {
                sessionCacheRepository.incrementRetryCount(op.id, op.retryCount);
                System.err.println("SyncService: Failed to sync operation " + op.id + ". Error: " + e.getMessage());
            }
        }
    }

    private boolean handlePendingOperation(SessionCacheRepository.PendingOperation op) throws Exception {
        switch (op.operationType) {
            case SessionCacheRepository.OP_USER_CREATE:
            case SessionCacheRepository.OP_USER_UPDATE:
                return handleUserUpsert(op);
            case SessionCacheRepository.OP_USER_DELETE:
                return handleUserDelete(op);
            case SessionCacheRepository.OP_FILE_CREATE:
            case SessionCacheRepository.OP_FILE_UPDATE:
                return handleFileUpsert(op);
            case SessionCacheRepository.OP_FILE_DELETE:
                return handleFileDelete(op);
            case SessionCacheRepository.OP_ACL_GRANT:
                return handleAclGrant(op);
            case SessionCacheRepository.OP_ACL_REVOKE:
                return handleAclRevoke(op);
            default:
                return true;
        }
    }

    private boolean handleUserUpsert(SessionCacheRepository.PendingOperation op) throws Exception {
        User localUser = gson.fromJson(op.payload, User.class);
        if (localUser == null) {
            return true;
        }
        userRepository.save(localUser);
        return true;
    }

    private boolean handleUserDelete(SessionCacheRepository.PendingOperation op) throws Exception {
        if (op.payload == null || op.payload.isBlank()) {
            return true;
        }
        User localUser = gson.fromJson(op.payload, User.class);
        String username = localUser != null ? localUser.getUsername() : op.fileId;
        if (username != null) {
            userRepository.deleteByUsername(username);
        }
        return true;
    }

    private boolean handleFileUpsert(SessionCacheRepository.PendingOperation op) throws Exception {
        if (shouldSkipForRemoteConflict(op.fileId)) {
            sessionCacheRepository.deletePendingOperationsForFile(op.fileId);
            return true;
        }

        FileMetadata meta = gson.fromJson(op.payload, FileMetadata.class);
        if (meta == null) {
            return true;
        }
        fileMetadataRepository.saveOrUpdate(meta.getId(), meta.getName(), meta.getOwner(), meta.getSizeBytes());
        sessionCacheRepository.markLocalFileSynced(op.fileId, System.currentTimeMillis());
        return true;
    }

    private boolean handleFileDelete(SessionCacheRepository.PendingOperation op) throws Exception {
        if (shouldSkipForRemoteConflict(op.fileId)) {
            sessionCacheRepository.deletePendingOperationsForFile(op.fileId);
            return true;
        }
        LoadBalancerClient.deleteFile(op.fileId);
        sessionCacheRepository.deleteLocalFile(op.fileId);
        return true;
    }

    private boolean handleAclGrant(SessionCacheRepository.PendingOperation op) throws Exception {
        Acl acl = gson.fromJson(op.payload, Acl.class);
        if (acl == null) {
            return true;
        }
        aclRepository.grantPermission(acl.getFileId(), acl.getUsername(), acl.getPermission());
        return true;
    }

    private boolean handleAclRevoke(SessionCacheRepository.PendingOperation op) throws Exception {
        Acl acl = gson.fromJson(op.payload, Acl.class);
        if (acl == null) {
            return true;
        }
        aclRepository.revokePermission(acl.getFileId(), acl.getUsername());
        return true;
    }

    private void pushLocalLogs() throws Exception {
        List<SessionCacheRepository.LocalLogEntry> logs = sessionCacheRepository.getUnsyncedLogs();
        for (SessionCacheRepository.LocalLogEntry entry : logs) {
            systemLogRepository.logEvent(entry.eventType, entry.userId, entry.description, entry.severity);
            sessionCacheRepository.markLogSynced(entry.id);
        }
    }

    private void pullRemoteMetadata() throws Exception {
        User currentUser = SessionState.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        long lastSync = sessionCacheRepository.getLatestFileSyncTimestamp();
        List<FileMetadataRepository.FileRecord> records =
            fileMetadataRepository.findAccessibleFilesSince(currentUser.getUsername(), lastSync);

        long now = System.currentTimeMillis();
        for (FileMetadataRepository.FileRecord record : records) {
            SessionCacheRepository.LocalFileRecord local = sessionCacheRepository.findLocalFile(record.fileId);
            if (local != null && !"synced".equalsIgnoreCase(local.syncStatus) &&
                record.getLastChangeTimestamp() > local.lastSync) {
                logConflict("Remote wins for file " + record.filename + " (" + record.fileId + ")");
                sessionCacheRepository.deletePendingOperationsForFile(record.fileId);
            }

            sessionCacheRepository.upsertLocalFileMetadata(
                record.fileId,
                record.ownerId,
                record.filename,
                record.fileSize,
                record.totalChunks,
                "synced",
                record.getLastChangeTimestamp(),
                now
            );
        }
    }

    private boolean shouldSkipForRemoteConflict(String fileId) throws Exception {
        SessionCacheRepository.LocalFileRecord local = sessionCacheRepository.findLocalFile(fileId);
        if (local == null || "synced".equalsIgnoreCase(local.syncStatus)) {
            return false;
        }

        FileMetadataRepository.FileRecord remote = fileMetadataRepository.findById(fileId);
        if (remote == null) {
            return false;
        }

        if (remote.getLastChangeTimestamp() > local.lastSync) {
            logConflict("Remote wins for file " + remote.filename + " (" + fileId + ")");
            sessionCacheRepository.upsertLocalFileMetadata(
                remote.fileId,
                remote.ownerId,
                remote.filename,
                remote.fileSize,
                remote.totalChunks,
                "synced",
                remote.getLastChangeTimestamp(),
                System.currentTimeMillis()
            );
            return true;
        }
        return false;
    }

    private void logConflict(String description) throws Exception {
        Long userId = null;
        if (SessionState.getInstance().getCurrentUser() != null) {
            userId = SessionState.getInstance().getCurrentUser().getId();
        }
        systemLogRepository.logEvent("SYNC_CONFLICT", userId, description, "WARNING");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
