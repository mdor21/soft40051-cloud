package com.ntu.cloudgui.app.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a logical file as seen by the GUI.
 *
 * - id: local UUID used in the GUI / in‑memory store
 * - name: display name
 * - ownerUsername: who owns the file
 * - deleted: soft delete flag
 * - createdAt / updatedAt: timestamps
 * - remoteFileId: ID used by Load Balancer / Aggregator for real storage
 */
public class FileMeta {

    private UUID id = UUID.randomUUID();
    private String name;
    private String ownerUsername;
    private boolean deleted;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // NEW: identifier in distributed storage (via Load Balancer / Aggregator)
    private String remoteFileId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
        touch();
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Update the last‑modified timestamp.
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }

    // -------- Remote storage integration --------

    /**
     * ID assigned by the backend (Load Balancer / Aggregator) for this file.
     * Used by FilesController + LoadBalancerClient for download operations.
     */
    public String getRemoteFileId() {
        return remoteFileId;
    }

    public void setRemoteFileId(String remoteFileId) {
        this.remoteFileId = remoteFileId;
        touch();
    }

    @Override
    public String toString() {
        return "FileMeta{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ownerUsername='" + ownerUsername + '\'' +
                ", deleted=" + deleted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", remoteFileId='" + remoteFileId + '\'' +
                '}';
    }
}
