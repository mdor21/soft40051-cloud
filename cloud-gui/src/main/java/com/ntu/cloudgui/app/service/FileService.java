package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.client.LoadBalancerClient;
import com.ntu.cloudgui.app.db.DatabaseManager;
import com.ntu.cloudgui.app.db.FileMetadataRepository;
import com.ntu.cloudgui.app.db.AclRepository;
import com.ntu.cloudgui.app.db.SessionCacheRepository;
import com.ntu.cloudgui.app.model.Acl;
import com.ntu.cloudgui.app.model.FileMetadata;
import com.ntu.cloudgui.app.session.SessionState;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FileService {

    private final FileMetadataRepository fileMetadataRepo = new FileMetadataRepository();
    private final AclRepository aclRepo = new AclRepository();
    private final SessionCacheRepository sessionCacheRepo = new SessionCacheRepository();
    private final LoggingService logger = LoggingService.getInstance();
    private final Gson gson = new Gson();

    public CompletableFuture<OperationResult> createFile(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            String username = getCurrentUsername();
            boolean online = DatabaseManager.isMysqlConnected();
            long now = System.currentTimeMillis();
            String fileId = filename;

            try {
                if (online) {
                    String warning = null;
                    try {
                        String lbFileId = uploadViaLoadBalancer(filename, new byte[0], resolveFileId(filename));
                        if (lbFileId != null && !lbFileId.isBlank()) {
                            fileId = lbFileId;
                        }
                    } catch (Exception e) {
                        warning = e.getMessage();
                    }
                    fileMetadataRepo.saveOrUpdate(fileId, filename, username, 0L);
                    sessionCacheRepo.upsertLocalFileMetadata(fileId, getCurrentUserId(), filename, 0L, 1,
                        "synced", now, now);
                    logger.log(username, "CREATE_FILE", "Created file " + filename, true);
                    if (warning != null) {
                        logger.log(username, "CREATE_FILE_STORAGE_WARNING", warning, false);
                        return OperationResult.success("File metadata created; storage warning: " + warning);
                    }
                    return OperationResult.success("File created: " + filename);
                }

                FileMetadata meta = new FileMetadata();
                meta.setId(fileId);
                meta.setName(filename);
                meta.setOwner(username);
                meta.setSizeBytes(0L);
                sessionCacheRepo.queueOperation(SessionCacheRepository.OP_FILE_CREATE, fileId, gson.toJson(meta));
                sessionCacheRepo.upsertLocalFileMetadata(fileId, getCurrentUserId(), filename, 0L, 1,
                    "pending", now, 0L);
                logger.log(username, "CREATE_FILE_OFFLINE", "Queued creation for file " + filename, true);
                return OperationResult.success("Queued file creation: " + filename);
            } catch (Exception e) {
                logger.log(username, "CREATE_FILE_ERROR", e.getMessage(), false);
                return OperationResult.failure("Create error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<OperationResult> saveFile(String filename, byte[] data) {
        return CompletableFuture.supplyAsync(() -> {
            String username = getCurrentUsername();
            boolean online = DatabaseManager.isMysqlConnected();
            long now = System.currentTimeMillis();
            String fileId = resolveFileId(filename);

            try {
                if (online) {
                    String warning = null;
                    try {
                        String lbFileId = uploadViaLoadBalancer(filename, data, resolveFileId(filename));
                        if (lbFileId != null && !lbFileId.isBlank()) {
                            fileId = lbFileId;
                        }
                    } catch (Exception e) {
                        warning = e.getMessage();
                    }
                    fileMetadataRepo.saveOrUpdate(fileId, filename, username, data.length);
                    sessionCacheRepo.upsertLocalFileMetadata(fileId, getCurrentUserId(), filename, data.length, 1,
                        "synced", now, now);
                    logger.log(username, "UPDATE_FILE", "Updated file " + filename, true);
                    if (warning != null) {
                        logger.log(username, "UPDATE_FILE_STORAGE_WARNING", warning, false);
                        return OperationResult.success("Saved metadata; storage warning: " + warning);
                    }
                    return OperationResult.success("Saved: " + filename);
                }

                FileMetadata meta = new FileMetadata();
                meta.setId(fileId);
                meta.setName(filename);
                meta.setOwner(username);
                meta.setSizeBytes(data.length);
                sessionCacheRepo.queueOperation(SessionCacheRepository.OP_FILE_UPDATE, fileId, gson.toJson(meta));
                sessionCacheRepo.upsertLocalFileMetadata(fileId, getCurrentUserId(), filename, data.length, 1,
                    "pending", now, 0L);
                logger.log(username, "UPDATE_FILE_OFFLINE", "Queued update for file " + filename, true);
                return OperationResult.success("Queued save: " + filename);
            } catch (Exception e) {
                logger.log(username, "UPDATE_FILE_ERROR", e.getMessage(), false);
                return OperationResult.failure("Save error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<OperationResult> uploadFile(String filename, byte[] data) {
        return CompletableFuture.supplyAsync(() -> {
            String username = getCurrentUsername();
            boolean online = DatabaseManager.isMysqlConnected();
            long now = System.currentTimeMillis();
            String fileId = filename;

            try {
                if (online) {
                    String warning = null;
                    try {
                        String lbFileId = uploadViaLoadBalancer(filename, data, resolveFileId(filename));
                        if (lbFileId != null && !lbFileId.isBlank()) {
                            fileId = lbFileId;
                        }
                    } catch (Exception e) {
                        warning = e.getMessage();
                    }
                    fileMetadataRepo.saveOrUpdate(fileId, filename, username, data.length);
                    sessionCacheRepo.upsertLocalFileMetadata(fileId, getCurrentUserId(), filename, data.length, 1,
                        "synced", now, now);
                    logger.log(username, "UPLOAD_FILE", "Uploaded file " + filename, true);
                    if (warning != null) {
                        logger.log(username, "UPLOAD_FILE_STORAGE_WARNING", warning, false);
                        return OperationResult.success("Uploaded metadata; storage warning: " + warning);
                    }
                    return OperationResult.success("Uploaded: " + filename);
                }

                FileMetadata meta = new FileMetadata();
                meta.setId(fileId);
                meta.setName(filename);
                meta.setOwner(username);
                meta.setSizeBytes(data.length);
                sessionCacheRepo.queueOperation(SessionCacheRepository.OP_FILE_CREATE, fileId, gson.toJson(meta));
                sessionCacheRepo.upsertLocalFileMetadata(fileId, getCurrentUserId(), filename, data.length, 1,
                    "pending", now, 0L);
                logger.log(username, "UPLOAD_FILE_OFFLINE", "Queued upload for file " + filename, true);
                return OperationResult.success("Queued upload: " + filename);
            } catch (Exception e) {
                logger.log(username, "UPLOAD_FILE_ERROR", e.getMessage(), false);
                return OperationResult.failure("Upload error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<OperationResult> deleteFile(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            String username = getCurrentUsername();
            boolean online = DatabaseManager.isMysqlConnected();
            String fileId = resolveFileId(filename);

            try {
                if (online) {
                    LoadBalancerClient.deleteFile(fileId);
                    sessionCacheRepo.deleteLocalFile(fileId);
                    logger.log(username, "DELETE_FILE", "Deleted file " + filename, true);
                    return OperationResult.success("File deleted: " + filename);
                }

                sessionCacheRepo.queueOperation(SessionCacheRepository.OP_FILE_DELETE, fileId,
                    "{\"filename\":\"" + filename + "\"}");
                sessionCacheRepo.deleteLocalFile(fileId);
                logger.log(username, "DELETE_FILE_OFFLINE", "Queued deletion for file " + filename, true);
                return OperationResult.success("Queued deletion: " + filename);
            } catch (Exception e) {
                logger.log(username, "DELETE_FILE_ERROR", e.getMessage(), false);
                return OperationResult.failure("Delete error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<OperationResult> shareFile(String filename, String targetUsername, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            String ownerUsername = getCurrentUsername();
            boolean online = DatabaseManager.isMysqlConnected();
            String fileId = resolveFileId(filename);

            Acl acl = new Acl();
            acl.setFileId(fileId);
            acl.setUsername(targetUsername);
            acl.setPermission(permission);

            try {
                if (online) {
                    aclRepo.grantPermission(fileId, targetUsername, permission);
                    logger.log(ownerUsername, "SHARE_FILE", "Shared file " + filename + " with " + targetUsername, true);
                    return OperationResult.success("File shared with " + targetUsername);
                }

                sessionCacheRepo.queueOperation(SessionCacheRepository.OP_ACL_GRANT, fileId, gson.toJson(acl));
                logger.log(ownerUsername, "SHARE_FILE_OFFLINE", "Queued share for file " + filename, true);
                return OperationResult.success("Queued share for " + targetUsername);
            } catch (Exception e) {
                logger.log(ownerUsername, "SHARE_FILE_ERROR", e.getMessage(), false);
                return OperationResult.failure("Share error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<DownloadResult> downloadFile(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            String fileId = resolveFileId(filename);
            try (InputStream input = LoadBalancerClient.downloadFile(fileId)) {
                byte[] data = input.readAllBytes();
                return DownloadResult.success(data);
            } catch (Exception e) {
                return DownloadResult.failure(e.getMessage());
            }
        });
    }

    public CompletableFuture<List<String>> listFilesForCurrentUser() {
        return CompletableFuture.supplyAsync(() -> {
            String username = getCurrentUsername();
            boolean online = DatabaseManager.isMysqlConnected();
            long now = System.currentTimeMillis();
            List<String> filenames = new ArrayList<>();

            try {
                if (online) {
                    List<FileMetadataRepository.FileRecord> records = fileMetadataRepo.findAccessibleFiles(username);
                    for (FileMetadataRepository.FileRecord record : records) {
                        sessionCacheRepo.upsertLocalFileMetadata(
                            record.fileId,
                            record.ownerId,
                            record.filename,
                            record.fileSize,
                            record.totalChunks,
                            "synced",
                            record.getLastChangeTimestamp(),
                            now
                        );
                        filenames.add(record.filename);
                    }
                    return filenames;
                }

                for (SessionCacheRepository.LocalFileRecord record : sessionCacheRepo.listLocalFiles(null)) {
                    filenames.add(record.originalFilename);
                }
                return filenames;
            } catch (Exception e) {
                logger.log(username, "LIST_FILES_ERROR", e.getMessage(), false);
                return filenames;
            }
        });
    }

    private String resolveFileId(String filename) {
        String username = getCurrentUsername();
        try {
            String id = fileMetadataRepo.findFileIdByNameAndOwner(filename, username);
            if (id != null) {
                return id;
            }
            SessionCacheRepository.LocalFileRecord local = sessionCacheRepo.findLocalFileByName(filename, getCurrentUserId());
            if (local != null && local.fileId != null) {
                return local.fileId;
            }
        } catch (SQLException e) {
            // Fall through to filename fallback.
        }
        return filename;
    }

    private String getCurrentUsername() {
        return SessionState.getInstance().getCurrentUser() == null
            ? "SYSTEM"
            : SessionState.getInstance().getCurrentUser().getUsername();
    }

    private long getCurrentUserId() {
        if (SessionState.getInstance().getCurrentUser() == null ||
            SessionState.getInstance().getCurrentUser().getId() == null) {
            return 0L;
        }
        return SessionState.getInstance().getCurrentUser().getId();
    }

    public static class OperationResult {
        public final boolean success;
        public final String message;

        private OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult failure(String message) {
            return new OperationResult(false, message);
        }
    }

    public static class DownloadResult {
        public final String status;
        public final String message;
        public final byte[] fileData;

        private DownloadResult(String status, String message, byte[] fileData) {
            this.status = status;
            this.message = message;
            this.fileData = fileData;
        }

        public static DownloadResult success(byte[] fileData) {
            return new DownloadResult("OK", "success", fileData);
        }

        public static DownloadResult failure(String message) {
            return new DownloadResult("ERROR", message, null);
        }
    }

    private String uploadViaLoadBalancer(String filename, byte[] data, String fileId) throws IOException {
        byte[] payload = data == null ? new byte[0] : data;
        try (InputStream input = new ByteArrayInputStream(payload)) {
            return LoadBalancerClient.uploadFile(input, filename, payload.length, fileId);
        }
    }
}
