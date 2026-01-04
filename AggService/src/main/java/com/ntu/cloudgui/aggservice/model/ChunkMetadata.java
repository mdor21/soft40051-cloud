package com.ntu.cloudgui.aggservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chunk_metadata")
public class ChunkMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String chunkId;
    private String fileId;
    private int chunkIndex;
    private String serverHost;
    private String remotePath;
    private long originalSize;
    private long encryptedSize;
    private long crc32;
    private LocalDateTime createdAt;

    public ChunkMetadata() {
    }

    public ChunkMetadata(String chunkId, String fileId, int chunkIndex, String serverHost, String remotePath, long originalSize, long encryptedSize, long crc32, LocalDateTime createdAt) {
        this.chunkId = chunkId;
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.serverHost = serverHost;
        this.remotePath = remotePath;
        this.originalSize = originalSize;
        this.encryptedSize = encryptedSize;
        this.crc32 = crc32;
        this.createdAt = createdAt;
    }

    public ChunkMetadata(String fileId, int chunkIndex, String serverHost, String remotePath, 
                        long originalSize, long encryptedSize, long crc32, LocalDateTime createdAt) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.serverHost = serverHost;
        this.remotePath = remotePath;
        this.originalSize = originalSize;
        this.encryptedSize = encryptedSize;
        this.crc32 = crc32;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getServerHost() { return serverHost; }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }
    public String getRemotePath() { return remotePath; }
    public void setRemotePath(String remotePath) { this.remotePath = remotePath; }
    public long getOriginalSize() { return originalSize; }
    public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }
    public long getEncryptedSize() { return encryptedSize; }
    public void setEncryptedSize(long encryptedSize) { this.encryptedSize = encryptedSize; }
    public long getCrc32() { return crc32; }
    public void setCrc32(long crc32) { this.crc32 = crc32; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
