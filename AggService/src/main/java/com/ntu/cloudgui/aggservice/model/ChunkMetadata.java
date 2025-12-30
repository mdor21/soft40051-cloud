package com.ntu.cloudgui.aggservice.model;

import java.time.LocalDateTime;
import java.util.Objects;

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
public class ChunkMetadata {

    private Long id;
    private String fileId;
    private Integer chunkIndex;
    private String serverHost;
    private String remotePath;
    private Long originalSize;
    private Long encryptedSize;
    private Long crc32;
    private LocalDateTime uploadTimestamp;

    /**
     * Constructor - Empty (for ORM/reflection)
     */
    public ChunkMetadata() {
    }

    /**
     * Constructor - Minimal (fileId and chunk index only)
     *
     * Used when querying chunks.
     *
     * @param fileId Unique file identifier (UUID)
     * @param chunkIndex Index of chunk in sequence (0-based)
     */
    public ChunkMetadata(String fileId, Integer chunkIndex) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
    }

    /**
     * Constructor - Full (all required fields)
     *
     * Used when creating new chunk metadata.
     *
     * @param fileId Unique file identifier (UUID)
     * @param chunkIndex Index of chunk in sequence (0-based)
     * @param serverHost Storage server hostname/IP
     * @param remotePath Path to chunk on storage server
     * @param originalSize Size of plaintext chunk in bytes
     * @param crc32 CRC32 checksum of plaintext chunk
     */
    public ChunkMetadata(String fileId, Integer chunkIndex, String serverHost,
                        String remotePath, Long originalSize, Long crc32) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.serverHost = serverHost;
        this.remotePath = remotePath;
        this.originalSize = originalSize;
        this.crc32 = crc32;
        this.uploadTimestamp = LocalDateTime.now();
    }

    /**
     * Constructor - Full with encrypted size and timestamp
     *
     * Used when retrieving metadata from database.
     *
     * @param fileId Unique file identifier (UUID)
     * @param chunkIndex Index of chunk in sequence (0-based)
     * @param serverHost Storage server hostname/IP
     * @param remotePath Path to chunk on storage server
     * @param originalSize Size of plaintext chunk in bytes
     * @param encryptedSize Size of encrypted chunk in bytes
     * @param crc32 CRC32 checksum of plaintext chunk
     * @param uploadTimestamp When chunk was uploaded
     */
    public ChunkMetadata(String fileId, Integer chunkIndex, String serverHost,
                        String remotePath, Long originalSize, Long encryptedSize,
                        Long crc32, LocalDateTime uploadTimestamp) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.serverHost = serverHost;
        this.remotePath = remotePath;
        this.originalSize = originalSize;
        this.encryptedSize = encryptedSize;
        this.crc32 = crc32;
        this.uploadTimestamp = uploadTimestamp;
    }

    // ==================== GETTERS ====================

    /**
     * Get database record ID
     *
     * @return Auto-increment ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Get unique file identifier
     *
     * @return UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000")
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * Get chunk index in sequence
     *
     * File is split into chunks, each with an index.
     * Chunks must be reassembled in order: 0, 1, 2, ...
     *
     * @return Chunk index (0-based)
     */
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    /**
     * Get storage server hostname or IP address
     *
     * @return Server address (e.g., "storage-server-1.cloud.local")
     */
    public String getServerHost() {
        return serverHost;
    }

    /**
     * Get path to chunk on storage server
     *
     * Remote path format: /chunks/{fileId}/{chunkIndex}
     * or: /data/agg/{year}/{month}/{fileId}_{chunkIndex}.enc
     *
     * @return Remote file path
     */
    public String getRemotePath() {
        return remotePath;
    }

    /**
     * Get original (plaintext) chunk size in bytes
     *
     * Size of data before encryption.
     * Typically 5MB (5242880 bytes) except for last chunk.
     *
     * @return Original chunk size in bytes
     */
    public Long getOriginalSize() {
        return originalSize;
    }

    /**
     * Get encrypted chunk size in bytes
     *
     * Size of data after encryption (may be slightly larger due to IV/padding).
     * Used for storage quota calculations.
     *
     * @return Encrypted chunk size in bytes
     */
    public Long getEncryptedSize() {
        return encryptedSize;
    }

    /**
     * Set encrypted chunk size
     *
     * @param encryptedSize Encrypted chunk size in bytes
     */
    public void setEncryptedSize(Long encryptedSize) {
        this.encryptedSize = encryptedSize;
    }

    /**
     * Get CRC32 checksum of original plaintext
     *
     * Used to verify chunk integrity during reconstruction.
     * Calculated BEFORE encryption for consistency.
     *
     * @return CRC32 checksum value
     */
    public Long getCrc32() {
        return crc32;
    }

    /**
     * Get upload timestamp
     *
     * @return DateTime when chunk was uploaded to storage
     */
    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    /**
     * Get age in seconds
     *
     * Useful for cleanup operations, archival, or analytics.
     *
     * @return Age in seconds since upload
     */
    public long getAgeSeconds() {
        if (uploadTimestamp == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.SECONDS.between(
            uploadTimestamp,
            LocalDateTime.now()
        );
    }

    /**
     * Check if chunk is complete
     *
     * A chunk is complete if all required fields are populated.
     *
     * @return true if chunk metadata is valid
     */
    public boolean isComplete() {
        return fileId != null && !fileId.isEmpty() &&
               chunkIndex != null && chunkIndex >= 0 &&
               serverHost != null && !serverHost.isEmpty() &&
               remotePath != null && !remotePath.isEmpty() &&
               originalSize != null && originalSize > 0 &&
               crc32 != null;
    }

    // ==================== SETTERS ====================

    /**
     * Set database record ID
     *
     * @param id Auto-increment ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Set unique file identifier
     *
     * @param fileId UUID string
     */
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    /**
     * Set chunk index
     *
     * @param chunkIndex Chunk index (0-based)
     */
    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    /**
     * Set storage server hostname
     *
     * @param serverHost Server address
     */
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    /**
     * Set remote path on storage server
     *
     * @param remotePath Remote file path
     */
    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    /**
     * Set original (plaintext) chunk size
     *
     * @param originalSize Original chunk size in bytes
     */
    public void setOriginalSize(Long originalSize) {
        this.originalSize = originalSize;
    }

    /**
     * Set CRC32 checksum
     *
     * @param crc32 CRC32 checksum value
     */
    public void setCrc32(Long crc32) {
        this.crc32 = crc32;
    }

    /**
     * Set upload timestamp
     *
     * @param uploadTimestamp DateTime of upload
     */
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

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
        return Objects.equals(fileId, other.fileId) &&
               Objects.equals(chunkIndex, other.chunkIndex);
    }

    /**
     * Get hash code based on fileId and chunkIndex
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(fileId, chunkIndex);
    }

    /**
     * Get string representation
     *
     * Format: ChunkMetadata{fileId=..., chunkIndex=..., size=..., crc32=...}
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return "ChunkMetadata{" +
               "id=" + id +
               ", fileId='" + fileId + '\'' +
               ", chunkIndex=" + chunkIndex +
               ", serverHost='" + serverHost + '\'' +
               ", remotePath='" + remotePath + '\'' +
               ", originalSize=" + originalSize +
               ", encryptedSize=" + encryptedSize +
               ", crc32=" + crc32 +
               ", uploadTimestamp=" + uploadTimestamp +
               '}';
    }
}
