package com.ntu.cloudgui.aggservice.model;

import java.time.LocalDateTime;

public class FileMetadata {
    private String fileId;
    private String originalName;
    private int totalChunks;
    private long sizeBytes;
    private String encryptionAlgo;
    private LocalDateTime uploadedAt;

    public FileMetadata() {
    }

    public FileMetadata(String fileId, String originalName, int totalChunks, long sizeBytes, String encryptionAlgo, LocalDateTime uploadedAt) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.totalChunks = totalChunks;
        this.sizeBytes = sizeBytes;
        this.encryptionAlgo = encryptionAlgo;
        this.uploadedAt = uploadedAt;
    }

    public boolean isValid() {
        return fileId != null && originalName != null && totalChunks > 0 && sizeBytes > 0;
    }

    // Getters and Setters
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getEncryptionAlgo() { return encryptionAlgo; }
    public void setEncryptionAlgo(String encryptionAlgo) { this.encryptionAlgo = encryptionAlgo; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
