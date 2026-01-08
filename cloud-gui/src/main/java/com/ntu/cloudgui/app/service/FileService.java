package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.db.DatabaseManager;
import com.ntu.cloudgui.app.db.FileMetadataRepository;
import com.ntu.cloudgui.app.db.AclRepository;
import com.ntu.cloudgui.app.db.SessionCacheRepository;
import com.ntu.cloudgui.app.model.FileMetadata;
import com.ntu.cloudgui.app.session.SessionState;
import com.google.gson.Gson;

import java.util.Collection;
import java.util.UUID;

public class FileService {

    private final FileMetadataRepository fileMetadataRepo = new FileMetadataRepository();
    private final AclRepository aclRepo = new AclRepository();
    private final SessionCacheRepository sessionCacheRepo = new SessionCacheRepository();
    private final LoggingService logger = LoggingService.getInstance();
    private final Gson gson = new Gson();

    public FileMetadata createFile(String name, String content) {
        String username = SessionState.getInstance().getCurrentUser().getUsername();
        FileMetadata meta = new FileMetadata();
        meta.setId(UUID.randomUUID().toString());
        meta.setName(name);
        meta.setOwner(username);
        meta.setSizeBytes(content.length());

        boolean online = DatabaseManager.isMysqlConnected();

        try {
            if (online) {
                fileMetadataRepo.save(meta.getId(), meta.getName(), meta.getOwner(), meta.getSizeBytes());
                // TODO: Upload content to file server
                logger.log(username, "CREATE_FILE", "Created file " + name, true);
            } else {
                // Offline: queue the creation
                String payload = gson.toJson(meta);
                sessionCacheRepo.queueOperation("CREATE", "FILE_METADATA", meta.getId(), payload);
                logger.log(username, "CREATE_FILE_OFFLINE", "Queued creation for file " + name, true);
            }
        } catch (Exception e) {
            logger.log(username, "CREATE_FILE_ERROR", e.getMessage(), false);
            return null;
        }
        return meta;
    }

    public void deleteFile(String fileId) {
        String username = SessionState.getInstance().getCurrentUser().getUsername();
        boolean online = DatabaseManager.isMysqlConnected();

        try {
            if (online) {
                // In a real app, you'd check for ownership/permissions first
                fileMetadataRepo.delete(fileId);
                // TODO: Delete content from file server
                logger.log(username, "DELETE_FILE", "Deleted file " + fileId, true);
            } else {
                String payload = "{\"id\":\"" + fileId + "\"}";
                sessionCacheRepo.queueOperation("DELETE", "FILE_METADATA", fileId, payload);
                logger.log(username, "DELETE_FILE_OFFLINE", "Queued deletion for file " + fileId, true);
            }
        } catch (Exception e) {
            logger.log(username, "DELETE_FILE_ERROR", e.getMessage(), false);
        }
    }

    // listFilesForCurrentUser would need to be updated to fetch from the remote DB
    // or local cache based on connectivity. Omitted for brevity.
}
