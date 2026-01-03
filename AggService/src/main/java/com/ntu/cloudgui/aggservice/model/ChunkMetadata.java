package com.ntu.cloudgui.aggservice.model;

import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * ChunkMetadata - Chunk Information Storage Model
 *
 * Represents metadata about an encrypted file chunk.
 * Stored in the chunk_metadata database table.
 *
 * Contains:
 * - Chunk identification (fileId, chunkIndex)
 * - Chunk properties (originalSize, encryptedSize)
 * - Storage location (serverHost, remotePath)
 * - Integrity verification (crc32)
 * - Timestamps (uploadTimestamp)
 *
 * Database Table:
 * CREATE TABLE chunk_metadata (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   fileId VARCHAR(36) NOT NULL,
 *   chunkIndex INT NOT NULL,
 *   serverHost VARCHAR(255) NOT NULL,
 *   remotePath VARCHAR(512) NOT NULL,
 *   originalSize BIGINT NOT NULL,
 *   encryptedSize BIGINT NOT NULL,
 *   crc32 BIGINT NOT NULL,
 *   uploadTimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *   UNIQUE KEY unique_file_chunk (fileId, chunkIndex),
 *   INDEX idx_fileId (fileId)
 * );
 */
@Entity
public class ChunkMetadata {

    @Id private String chunkId;
    private String filename;
    private int chunkIndex;
    private int totalChunks;
    private String checksum;
    private int fileServerIndex;
    private LocalDateTime createdAt;

    // ==================== OBJECT METHODS ====================

    /**
     * Check equality based on fileId and chunkIndex
     *
     * Two chunks are equal if they represent the same file chunk.
     *
     * @param obj Object to compare
     * @return true if equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ChunkMetadata other = (ChunkMetadata) obj;
        return Objects.equals(chunkId, other.chunkId);
    }

    /**
     * Get hash code based on chunkId
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(chunkId);
    }

    /**
     * Get string representation
     *
     * Format: ChunkMetadata{chunkId=..., filename=..., chunkIndex=..., totalChunks=..., checksum=..., fileServerIndex=..., createdAt=...}
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return "ChunkMetadata{" +
               "chunkId='" + chunkId + '\'' +
               ", filename='" + filename + '\'' +
               ", chunkIndex=" + chunkIndex +
               ", totalChunks=" + totalChunks +
               ", checksum='" + checksum + '\'' +
               ", fileServerIndex=" + fileServerIndex +
               ", createdAt=" + createdAt +
               '}';
    }
}

@Entity
public class FileMetadata {
    @Id private String fileId;
    private String filename;
    private long fileSize;
    private String ownerUsername;
    private String checksum;
    private LocalDateTime uploadedAt;

    @Override
    public String toString() {
        return "FileMetadata{" +
               "fileId='" + fileId + '\'' +
               ", filename='" + filename + '\'' +
               ", fileSize=" + fileSize +
               ", ownerUsername='" + ownerUsername + '\'' +
               ", checksum='" + checksum + '\'' +
               ", uploadedAt=" + uploadedAt +
               '}';
    }
}

@Entity
public class AccessControlList {
    @Id private String aclId;
    private String fileId;
    private String username;
    private String permission; // READ, WRITE, ADMIN
    private LocalDateTime grantedAt;

    @Override
    public String toString() {
        return "AccessControlList{" +
               "aclId='" + aclId + '\'' +
               ", fileId='" + fileId + '\'' +
               ", username='" + username + '\'' +
               ", permission='" + permission + '\'' +
               ", grantedAt=" + grantedAt +
               '}';
    }
}
