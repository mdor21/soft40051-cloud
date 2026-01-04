package com.ntu.cloudgui.aggservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    public FileMetadata(String originalName, String encryptionAlgo, int totalChunks, long sizeBytes) {
        this.originalName = originalName;
        this.encryptionAlgo = encryptionAlgo;
        this.totalChunks = totalChunks;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = LocalDateTime.now();
    }

    // This constructor is added to match the call from FileProcessingService
    public FileMetadata(String originalName, String encryptionAlgo, int totalChunks, long sizeBytes, String key) {
        this.originalName = originalName;
        this.encryptionAlgo = encryptionAlgo;
        this.totalChunks = totalChunks;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = LocalDateTime.now();
        // The 5th argument `key` is ignored for now, but this resolves the constructor error.
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
