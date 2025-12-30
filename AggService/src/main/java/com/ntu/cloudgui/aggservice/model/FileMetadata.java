package com.ntu.cloudgui.aggservice.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * FileMetadata - File Information Storage Model
 * 
 * Represents metadata about an uploaded file.
 * Stored in the file_metadata database table.
 * 
 * Contains:
 * - File identification (fileId, originalName)
 * - File properties (totalChunks, sizeBytes)
 * - Encryption information (encryptionAlgo)
 * - Timestamps (uploadTimestamp)
 * 
 * Database Table:
 * CREATE TABLE file_metadata (
 *   fileId VARCHAR(36) PRIMARY KEY,
 *   originalName VARCHAR(255) NOT NULL,
 *   totalChunks INT NOT NULL,
 *   sizeBytes BIGINT NOT NULL,
 *   encryptionAlgo VARCHAR(50),
 *   uploadTimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 * 
 * Example:
 * <pre>
 * FileMetadata metadata = new FileMetadata(
 *     "550e8400-e29b-41d4-a716-446655440000",
 *     "document.pdf",
 *     10,
 *     10485760,
 *     "AES/256/GCM"
 * );
 * 
 * fileMetadataRepository.save(metadata);
 * </pre>
 */
public class FileMetadata {
    
    // Properties
    private String fileId;
    private String originalName;
    private Integer totalChunks;
    private Long sizeBytes;
    private String encryptionAlgo;
    private LocalDateTime uploadTimestamp;
    
    /**
     * Constructor - Empty (for ORM/reflection)
     */
    public FileMetadata() {
    }
    
    /**
     * Constructor - Minimal (fileId only)
     * 
     * Used when fileId is the only required information.
     * 
     * @param fileId Unique file identifier (UUID)
     */
    public FileMetadata(String fileId) {
        this.fileId = fileId;
    }
    
    /**
     * Constructor - Full (all required fields)
     * 
     * Used when creating new file metadata for upload.
     * 
     * @param fileId Unique file identifier (UUID)
     * @param originalName Original filename from upload
     * @param totalChunks Total number of chunks file was split into
     * @param sizeBytes Total file size in bytes
     * @param encryptionAlgo Encryption algorithm used (e.g., "AES/256/GCM")
     */
    public FileMetadata(String fileId, String originalName, Integer totalChunks, 
                       Long sizeBytes, String encryptionAlgo) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.totalChunks = totalChunks;
        this.sizeBytes = sizeBytes;
        this.encryptionAlgo = encryptionAlgo;
        this.uploadTimestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor - Full with timestamp
     * 
     * Used when retrieving metadata from database.
     * 
     * @param fileId Unique file identifier (UUID)
     * @param originalName Original filename from upload
     * @param totalChunks Total number of chunks file was split into
     * @param sizeBytes Total file size in bytes
     * @param encryptionAlgo Encryption algorithm used
     * @param uploadTimestamp When file was uploaded
     */
    public FileMetadata(String fileId, String originalName, Integer totalChunks,
                       Long sizeBytes, String encryptionAlgo, LocalDateTime uploadTimestamp) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.totalChunks = totalChunks;
        this.sizeBytes = sizeBytes;
        this.encryptionAlgo = encryptionAlgo;
        this.uploadTimestamp = uploadTimestamp;
    }
    
    // ==================== GETTERS ====================
    
    /**
     * Get unique file identifier
     * 
     * @return UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000")
     */
    public String getFileId() {
        return fileId;
    }
    
    /**
     * Get original filename from upload
     * 
     * @return Filename as uploaded by user
     */
    public String getOriginalName() {
        return originalName;
    }
    
    /**
     * Get total number of chunks
     * 
     * File is split into 1MB chunks for storage.
     * This indicates how many chunks exist for this file.
     * 
     * @return Number of chunks (1-5000)
     */
    public Integer getTotalChunks() {
        return totalChunks;
    }
    
    /**
     * Get total file size in bytes
     * 
     * @return File size in bytes
     */
    public Long getSizeBytes() {
        return sizeBytes;
    }
    
    /**
     * Get human-readable file size
     * 
     * Converts bytes to KB, MB, or GB.
     * 
     * @return Size string (e.g., "5.2 MB")
     */
    public String getReadableSize() {
        if (sizeBytes == null) {
            return "Unknown";
        }
        
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        
        long kb = sizeBytes / 1024;
        if (kb < 1024) {
            return kb + " KB";
        }
        
        long mb = kb / 1024;
        if (mb < 1024) {
            return mb + " MB";
        }
        
        long gb = mb / 1024;
        return gb + " GB";
    }
    
    /**
     * Get encryption algorithm
     * 
     * @return Algorithm name (e.g., "AES/256/GCM")
     */
    public String getEncryptionAlgo() {
        return encryptionAlgo;
    }
    
    /**
     * Get upload timestamp
     * 
     * @return DateTime when file was uploaded
     */
    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }
    
    /**
     * Get age in seconds
     * 
     * Useful for cleanup operations.
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
    
    // ==================== SETTERS ====================
    
    /**
     * Set unique file identifier
     * 
     * @param fileId UUID string
     */
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    /**
     * Set original filename
     * 
     * @param originalName Filename
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    /**
     * Set total number of chunks
     * 
     * @param totalChunks Number of chunks
     */
    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    /**
     * Set file size in bytes
     * 
     * @param sizeBytes File size
     */
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    /**
     * Set encryption algorithm
     * 
     * @param encryptionAlgo Algorithm name
     */
    public void setEncryptionAlgo(String encryptionAlgo) {
        this.encryptionAlgo = encryptionAlgo;
    }
    
    /**
     * Set upload timestamp
     * 
     * @param uploadTimestamp DateTime of upload
     */
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
    
    // ==================== VALIDATION ====================
    
    /**
     * Validate metadata for completeness
     * 
     * Checks that all required fields are populated.
     * Used before saving to database.
     * 
     * @return true if all required fields present
     */
    public boolean isValid() {
        return fileId != null && !fileId.isEmpty() &&
               originalName != null && !originalName.isEmpty() &&
               totalChunks != null && totalChunks > 0 &&
               sizeBytes != null && sizeBytes > 0 &&
               encryptionAlgo != null && !encryptionAlgo.isEmpty();
    }
    
    /**
     * Get validation error message if invalid
     * 
     * @return Error message or null if valid
     */
    public String getValidationError() {
        if (fileId == null || fileId.isEmpty()) {
            return "fileId is required";
        }
        if (originalName == null || originalName.isEmpty()) {
            return "originalName is required";
        }
        if (totalChunks == null || totalChunks <= 0) {
            return "totalChunks must be positive";
        }
        if (sizeBytes == null || sizeBytes <= 0) {
            return "sizeBytes must be positive";
        }
        if (encryptionAlgo == null || encryptionAlgo.isEmpty()) {
            return "encryptionAlgo is required";
        }
        return null;
    }
    
    // ==================== OBJECT METHODS ====================
    
    /**
     * Check equality based on fileId
     * 
     * Two FileMetadata objects are equal if they have the same fileId.
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
        FileMetadata other = (FileMetadata) obj;
        return Objects.equals(fileId, other.fileId);
    }
    
    /**
     * Get hash code based on fileId
     * 
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(fileId);
    }
    
    /**
     * Get string representation
     * 
     * Format: FileMetadata{fileId=..., name=..., size=...}
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "FileMetadata{" +
               "fileId='" + fileId + '\'' +
               ", originalName='" + originalName + '\'' +
               ", totalChunks=" + totalChunks +
               ", sizeBytes=" + sizeBytes +
               ", encryptionAlgo='" + encryptionAlgo + '\'' +
               ", uploadTimestamp=" + uploadTimestamp +
               '}';
    }
}
