package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.model.FileMeta;
import com.ntu.cloudgui.app.session.InMemoryStore;
import com.ntu.cloudgui.app.session.SessionState;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class FileService {

    private final InMemoryStore store = InMemoryStore.getInstance();
    private final LoggingService logger = LoggingService.getInstance();

    public FileMeta createFile(String name, String content) {
        FileMeta meta = new FileMeta();
        meta.setName(name);
        meta.setOwnerUsername(SessionState.getInstance().getCurrentUser().getUsername());
        store.saveFile(meta);
        // TODO integrate with load balancer + remote file servers
        logger.log(meta.getOwnerUsername(), "CREATE_FILE",
                "Created file " + name, true);
        return meta;
    }

    public void updateFile(UUID id, String newContent) {
        Optional<FileMeta> opt = store.findFileById(id);
        opt.ifPresent(meta -> {
            // TODO ACL checks + remote write
            meta.touch();
            store.saveFile(meta);
            logger.log(SessionState.getInstance().getCurrentUser().getUsername(),
                    "UPDATE_FILE", "Updated file " + meta.getName(), true);
        });
    }

    public void deleteFile(UUID id) {
        Optional<FileMeta> opt = store.findFileById(id);
        opt.ifPresent(meta -> {
            store.deleteFile(id);
            // TODO remote delete with locking
            logger.log(SessionState.getInstance().getCurrentUser().getUsername(),
                    "DELETE_FILE", "Deleted file " + meta.getName(), true);
        });
    }

    public Collection<FileMeta> listFilesForCurrentUser() {
        // TODO: respect ACLs when real DB is wired
        return store.getAllFiles();
    }
}
