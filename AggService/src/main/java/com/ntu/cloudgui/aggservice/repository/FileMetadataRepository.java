package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.exception.DatabaseException;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * FileMetadataRepository - File Metadata Data Access Object
 * 
 * Provides CRUD operations for file_metadata table.
 * Handles all database interactions for file metadata.
 * 
 * Responsibilities:
 * - Save file metadata (INSERT)
 * - Retrieve file metadata (SELECT)
 * - Update file metadata (UPDATE)
 * - Delete file metadata (DELETE)
 * - Error handling and logging
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
 * HikariDataSource dataSource = dbConfig.getDataSource();
 * FileMetadataRepository repository = new FileMetadataRepository(dataSource);
 * 
 * FileMetadata metadata = new FileMetadata(...);
 * repository.save(metadata);
 * 
 * FileMetadata retrieved = repository.findById(fileId);
 * </pre>
 */
public class FileMetadataRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileMetadataRepository.class);
    
    private final HikariDataSource dataSource;
    
    // SQL Queries
    private static final String INSERT_QUERY =
        "INSERT INTO file_metadata " +
        "(fileId, originalName, totalChunks, sizeBytes, encryptionAlgo) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String SELECT_BY_ID_QUERY =
        "SELECT fileId, originalName, totalChunks, sizeBytes, encryptionAlgo, uploadTimestamp " +
        "FROM file_metadata WHERE fileId = ?";
    
    private static final String UPDATE_QUERY =
        "UPDATE file_metadata SET " +
        "originalName = ?, totalChunks = ?, sizeBytes = ?, encryptionAlgo = ? " +
        "WHERE fileId = ?";
    
    private static final String DELETE_QUERY =
        "DELETE FROM file_metadata WHERE fileId = ?";
    
    private static final String EXISTS_QUERY =
        "SELECT 1 FROM file_metadata WHERE fileId = ? LIMIT 1";
    
    /**
     * Constructor - Initialize repository with data source
     * 
     * @param dataSource HikariCP data source (connection pool)
     */
    public FileMetadataRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Save file metadata to database
     * 
     * Inserts a new file metadata record.
     * Validates metadata before saving.
     * 
     * @param metadata FileMetadata object to save
     * @throws DatabaseException if save fails
     */
    public void save(FileMetadata metadata) throws DatabaseException {
        // Validate metadata
        if (!metadata.isValid()) {
            throw new DatabaseException(
                DatabaseException.ErrorType.CONSTRAINT_VIOLATION,
                "file_metadata"
            );
        }
        
        logger.debug("Saving file metadata: {}", metadata.getFileId());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(INSERT_QUERY);
            
            // Set parameters
            stmt.setString(1, metadata.getFileId());
            stmt.setString(2, metadata.getOriginalName());
            stmt.setInt(3, metadata.getTotalChunks());
            stmt.setLong(4, metadata.getSizeBytes());
            stmt.setString(5, metadata.getEncryptionAlgo());
            
            // Execute insert
            int rowsInserted = stmt.executeUpdate();
            
            if (rowsInserted > 0) {
                logger.info("✓ File metadata saved: {}", metadata.getFileId());
            } else {
                throw new DatabaseException(
                    DatabaseException.ErrorType.UPDATE_FAILED,
                    "file_metadata"
                );
            }
            
        } catch (SQLException e) {
            logger.error("✗ Failed to save file metadata: {}", e.getMessage(), e);
            
            // Handle specific SQL errors
            if (e.getErrorCode() == 1062) {
                // Duplicate key error
                throw new DatabaseException(
                    DatabaseException.ErrorType.DUPLICATE_KEY,
                    "file_metadata",
                    e
                );
            }
            
            throw new DatabaseException(
                DatabaseException.ErrorType.QUERY_FAILED,
                "file_metadata",
                e
            );
            
        } finally {
            closeResources(stmt, conn);
        }
    }
    
    /**
     * Find file metadata by fileId
     * 
     * Retrieves file metadata for the given fileId.
     * Returns null if file not found.
     * 
     * @param fileId File identifier (UUID)
     * @return FileMetadata object or null if not found
     * @throws DatabaseException if query fails
     */
    public FileMetadata findById(String fileId) throws DatabaseException {
        logger.debug("Finding file metadata: {}", fileId);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(SELECT_BY_ID_QUERY);
            
            // Set parameter
            stmt.setString(1, fileId);
            
            // Execute query
            rs = stmt.executeQuery();
            
            // Check if result found
            if (rs.next()) {
                FileMetadata metadata = mapResultSetToFileMetadata(rs);
                logger.debug("✓ File metadata found: {}", fileId);
                return metadata;
            }
            
            logger.debug("File metadata not found: {}", fileId);
            return null;
            
        } catch (SQLException e) {
            logger.error("✗ Failed to find file metadata: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.QUERY_FAILED,
                "file_metadata",
                e
            );
            
        } finally {
            closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Update file metadata
     * 
     * Updates an existing file metadata record.
     * fileId cannot be changed (primary key).
     * 
     * @param metadata Updated FileMetadata object
     * @throws DatabaseException if update fails
     */
    public void update(FileMetadata metadata) throws DatabaseException {
        // Validate metadata
        if (!metadata.isValid()) {
            throw new DatabaseException(
                DatabaseException.ErrorType.CONSTRAINT_VIOLATION,
                "file_metadata"
            );
        }
        
        logger.debug("Updating file metadata: {}", metadata.getFileId());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(UPDATE_QUERY);
            
            // Set parameters
            stmt.setString(1, metadata.getOriginalName());
            stmt.setInt(2, metadata.getTotalChunks());
            stmt.setLong(3, metadata.getSizeBytes());
            stmt.setString(4, metadata.getEncryptionAlgo());
            stmt.setString(5, metadata.getFileId());
            
            // Execute update
            int rowsUpdated = stmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                logger.info("✓ File metadata updated: {}", metadata.getFileId());
            } else {
                throw new DatabaseException(
                    DatabaseException.ErrorType.NOT_FOUND,
                    "file_metadata"
                );
            }
            
        } catch (SQLException e) {
            logger.error("✗ Failed to update file metadata: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.UPDATE_FAILED,
                "file_metadata",
                e
            );
            
        } finally {
            closeResources(stmt, conn);
        }
    }
    
    /**
     * Delete file metadata
     * 
     * Deletes a file metadata record and all associated chunk metadata
     * (due to foreign key cascade).
     * 
     * @param fileId File identifier (UUID)
     * @throws DatabaseException if delete fails
     */
    public void deleteById(String fileId) throws DatabaseException {
        logger.debug("Deleting file metadata: {}", fileId);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(DELETE_QUERY);
            
            // Set parameter
            stmt.setString(1, fileId);
            
            // Execute delete
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted > 0) {
                logger.info("✓ File metadata deleted: {}", fileId);
            } else {
                logger.warn("File metadata not found: {}", fileId);
            }
            
        } catch (SQLException e) {
            logger.error("✗ Failed to delete file metadata: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.DELETE_FAILED,
                "file_metadata",
                e
            );
            
        } finally {
            closeResources(stmt, conn);
        }
    }
    
    /**
     * Check if file metadata exists
     * 
     * Efficiently checks if a file exists without loading full data.
     * 
     * @param fileId File identifier (UUID)
     * @return true if file exists
     * @throws DatabaseException if query fails
     */
    public boolean existsById(String fileId) throws DatabaseException {
        logger.debug("Checking if file exists: {}", fileId);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(EXISTS_QUERY);
            
            // Set parameter
            stmt.setString(1, fileId);
            
            // Execute query
            rs = stmt.executeQuery();
            
            boolean exists = rs.next();
            logger.debug("File exists: {} - {}", fileId, exists);
            
            return exists;
            
        } catch (SQLException e) {
            logger.error("✗ Failed to check file existence: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.QUERY_FAILED,
                "file_metadata",
                e
            );
            
        } finally {
            closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Map ResultSet row to FileMetadata object
     * 
     * Converts database row to domain object.
     * 
     * @param rs ResultSet positioned at data row
     * @return FileMetadata object
     * @throws SQLException if column access fails
     */
    private FileMetadata mapResultSetToFileMetadata(ResultSet rs) throws SQLException {
        LocalDateTime uploadTimestamp = null;
        Timestamp dbTimestamp = rs.getTimestamp("uploadTimestamp");
        if (dbTimestamp != null) {
            uploadTimestamp = dbTimestamp.toLocalDateTime();
        }
        
        return new FileMetadata(
            rs.getString("fileId"),
            rs.getString("originalName"),
            rs.getInt("totalChunks"),
            rs.getLong("sizeBytes"),
            rs.getString("encryptionAlgo"),
            uploadTimestamp
        );
    }
    
    /**
     * Close database resources
     * 
     * Safely closes ResultSet, PreparedStatement, and Connection.
     * 
     * @param rs ResultSet to close (may be null)
     * @param stmt PreparedStatement to close (may be null)
     * @param conn Connection to close (returns to pool)
     */
    private void closeResources(ResultSet rs, PreparedStatement stmt, Connection conn) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            logger.debug("Error closing ResultSet", e);
        }
        
        closeResources(stmt, conn);
    }
    
    /**
     * Close database resources
     * 
     * Safely closes PreparedStatement and Connection.
     * 
     * @param stmt PreparedStatement to close (may be null)
     * @param conn Connection to close (returns to pool)
     */
    private void closeResources(PreparedStatement stmt, Connection conn) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            logger.debug("Error closing PreparedStatement", e);
        }
        
        try {
            if (conn != null) {
                conn.close();  // Returns connection to pool
            }
        } catch (SQLException e) {
            logger.debug("Error closing Connection", e);
        }
    }
}
