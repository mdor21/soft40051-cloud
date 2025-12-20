package com.ntu.cloudgui.aggservice.model;

import java.time.Instant;

/**
 * High-level metadata for a stored file.
 *
 * Maps to a table in the remote MySQL database.
 */
public class FileMetadata {

    private String fileId;            // UUID used by LB / GUI
    private String originalName;      // user-provided name
    private int totalChunks;          // number of chunks
    private long sizeBytes;           // total size in bytes
    private String encryptionAlgo;    // e.g. "AES/GCM/NoPadding"
    private Instant uploadTimestamp = Instant.now();

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getEncryptionAlgo() {
        return encryptionAlgo;
    }

    public void setEncryptionAlgo(String encryptionAlgo) {
        this.encryptionAlgo = encryptionAlgo;
    }

    public Instant getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(Instant uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
}
