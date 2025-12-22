package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.exception.DatabaseException;
import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ChunkMetadataRepository - Chunk Metadata Data Access Object
 * 
 * Provides CRUD operations for chunk_metadata table.
 * Handles all database interactions for chunk metadata.
 * 
 * Responsibilities:
 * - Save chunk metadata (INSERT)
 * - Retrieve chunk metadata (SELECT)
 * - Delete chunk metadata (DELETE)
 * - Query by file and chunk index
 * - Error handling and logging
 * 
 * Database Table:
 * CREATE TABLE chunk_metadata (
 *   id INT AUTO_INCREMENT PRIMARY KEY,
 *   fileId VARCHAR(36) NOT NULL,
 *   chunkIndex INT NOT NULL,
 *   serverHost VARCHAR(50) NOT NULL,
 *   remotePath VARCHAR(255) NOT NULL,
 *   crc32Checksum BIGINT NOT NULL,
 *   sizeBytes BIGINT NOT NULL,
 *   uploadTimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *   UNIQUE KEY uk_file_chunk (fileId, chunkIndex),
 *   FOREIGN KEY (fileId) REFERENCES file_metadata(fileId) ON DELETE CASCADE
 * );
 * 
 * Example:
 * <pre>
 * HikariDataSource dataSource = dbConfig.getDataSource();
 * ChunkMetadataRepository repository = new ChunkMetadataRepository(dataSource);
 * 
 * ChunkMetadata chunk = new ChunkMetadata(...);
 * repository.save(chunk);
 * 
 * List<ChunkMetadata> chunks = repository.findByFileIdOrderByIndex(fileId);
 * </pre>
 */
public class ChunkMetadataRepository {
    private static final Logger logger = LoggerFactory.getLogger(ChunkMetadataRepository.class);
    
    private final HikariDataSource dataSource;
    
    // SQL Queries
    private static final String INSERT_QUERY =
        "INSERT INTO chunk_metadata " +
        "(fileId, chunkIndex, serverHost, remotePath, crc32Checksum, sizeBytes) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_BY_FILE_QUERY =
        "SELECT id, fileId, chunkIndex, serverHost, remotePath, crc32Checksum, sizeBytes, uploadTimestamp " +
        "FROM chunk_metadata WHERE fileId = ? ORDER BY chunkIndex ASC";
    
    private static final String SELECT_BY_FILE_AND_INDEX_QUERY =
        "SELECT id, fileId, chunkIndex, serverHost, remotePath, crc32Checksum, sizeBytes, uploadTimestamp " +
        "FROM chunk_metadata WHERE fileId = ? AND chunkIndex = ? LIMIT 1";
    
    private static final String DELETE_BY_FILE_QUERY =
        "DELETE FROM chunk_metadata WHERE fileId = ?";
    
    private static final String DELETE_BY_FILE_AND_INDEX_QUERY =
        "DELETE FROM chunk_metadata WHERE fileId = ? AND chunkIndex = ?";
    
    private static final String COUNT_BY_FILE_QUERY =
        "SELECT COUNT(*) as count FROM chunk_metadata WHERE fileId = ?";
    
    /**
     * Constructor - Initialize repository with data source
     * 
     * @param dataSource HikariCP data source (connection pool)
     */
    public ChunkMetadataRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Save chunk metadata to database
     * 
     * Inserts a new chunk metadata record.
     * Validates metadata before saving.
     * 
     * @param metadata ChunkMetadata object to save
     * @throws DatabaseException if save fails
     */
    public void save(ChunkMetadata metadata) throws DatabaseException {
        // Validate metadata
        if (!metadata.isValid()) {
            throw new DatabaseException(
                DatabaseException.ErrorType.CONSTRAINT_VIOLATION,
                "chunk_metadata"
            );
        }
        
        logger.debug("Saving chunk metadata: {}[{}]", 
                    metadata.getFileId(), metadata.getChunkIndex());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(INSERT_QUERY);
            
            // Set parameters
            stmt.setString(1, metadata.getFileId());
            stmt.setInt(2, metadata.getChunkIndex());
            stmt.setString(3, metadata.getServerHost());
            stmt.setString(4, metadata.getRemotePath());
            stmt.setLong(5, metadata.getCrc32Checksum());
            stmt.setLong(6, metadata.getSizeBytes());
            
            // Execute insert
            int rowsInserted = stmt.executeUpdate();
            
            if (rowsInserted > 0) {
                logger.info("✓ Chunk metadata saved: {}[{}]", 
                           metadata.getFileId(), metadata.getChunkIndex());
            } else {
                throw new DatabaseException(
                    DatabaseException.ErrorType.UPDATE_FAILED,
                    "chunk_metadata"
                );
            }
            
        } catch (SQLException e) {
            logger.error("✗ Failed to save chunk metadata: {}", e.getMessage(), e);
            
            // Handle specific SQL errors
            if (e.getErrorCode() == 1062) {
                // Duplicate key error
                throw new DatabaseException(
                    DatabaseException.ErrorType.DUPLICATE_KEY,
                    "chunk_metadata",
                    e
                );
            }
            
            throw new DatabaseException(
                DatabaseException.ErrorType.QUERY_FAILED,
                "chunk_metadata",
                e
            );
            
        } finally {
            closeResources(stmt, conn);
        }
    }
    
    /**
     * Find all chunks for a file, ordered by index
     * 
     * Retrieves all chunk metadata for a given file ID.
     * Results are ordered by chunk index (0, 1, 2, ...).
     * 
     * @param fileId File identifier (UUID)
     * @return List of ChunkMetadata ordered by index (empty if none found)
     * @throws DatabaseException if query fails
     */
    public List<ChunkMetadata> findByFileIdOrderByIndex(String fileId) 
            throws DatabaseException {
        logger.debug("Finding chunks for file: {}", fileId);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(SELECT_BY_FILE_QUERY);
            
            // Set parameter
            stmt.setString(1, fileId);
            
            // Execute query
            rs = stmt.executeQuery();
            
            // Build list
            List<ChunkMetadata> chunks = new ArrayList<>();
            while (rs.next()) {
                chunks.add(mapResultSetToChunkMetadata(rs));
            }
            
            logger.debug("✓ Found {} chunks for file: {}", chunks.size(), fileId);
            return chunks;
            
        } catch (SQLException e) {
            logger.error("✗ Failed to find chunks: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.QUERY_FAILED,
                "chunk_metadata",
                e
            );
            
        } finally {
            closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Find specific chunk by file ID and chunk index
     * 
     * Retrieves a single chunk by file ID and chunk number.
     * Returns null if not found.
     * 
     * @param fileId File identifier (UUID)
     * @param chunkIndex Zero-based chunk number
     * @return ChunkMetadata object or null if not found
     * @throws DatabaseException if query fails
     */
    public ChunkMetadata findByFileIdAndIndex(String fileId, Integer chunkIndex) 
            throws DatabaseException {
        logger.debug("Finding chunk: {}[{}]", fileId, chunkIndex);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(SELECT_BY_FILE_AND_INDEX_QUERY);
            
            // Set parameters
            stmt.setString(1, fileId);
            stmt.setInt(2, chunkIndex);
            
            // Execute query
            rs = stmt.executeQuery();
            
            // Check if result found
            if (rs.next()) {
                ChunkMetadata chunk = mapResultSetToChunkMetadata(rs);
                logger.debug("✓ Chunk found: {}[{}]", fileId, chunkIndex);
                return chunk;
            }
            
            logger.debug("Chunk not found: {}[{}]", fileId, chunkIndex);
            return null;
            
        } catch (SQLException e) {
            logger.error("✗ Failed to find chunk: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.QUERY_FAILED,
                "chunk_metadata",
                e
            );
            
        } finally {
            closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Count chunks for a file
     * 
     * Returns the total number of chunks stored for a file.
     * Useful for validation and integrity checks.
     * 
     * @param fileId File identifier (UUID)
     * @return Number of chunks (0 if none)
     * @throws DatabaseException if query fails
     */
    public int countByFileId(String fileId) throws DatabaseException {
        logger.debug("Counting chunks for file: {}", fileId);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(COUNT_BY_FILE_QUERY);
            
            // Set parameter
            stmt.setString(1, fileId);
            
            // Execute query
            rs = stmt.executeQuery();
            
            int count = 0;
            if (rs.next()) {
                count = rs.getInt("count");
            }
            
            logger.debug("✓ File has {} chunks", count);
            return count;
            
        } catch (SQLException e) {
            logger.error("✗ Failed to count chunks: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.QUERY_FAILED,
                "chunk_metadata",
                e
            );
            
        } finally {
            closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * Delete all chunks for a file
     * 
     * Deletes all chunk metadata records for the given file.
     * Also deletes physical chunks from storage (manual cleanup required).
     * 
     * @param fileId File identifier (UUID)
     * @throws DatabaseException if delete fails
     */
    public void deleteByFileId(String fileId) throws DatabaseException {
        logger.debug("Deleting all chunks for file: {}", fileId);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(DELETE_BY_FILE_QUERY);
            
            // Set parameter
            stmt.setString(1, fileId);
            
            // Execute delete
            int rowsDeleted = stmt.executeUpdate();
            
            logger.info("✓ Deleted {} chunk records for file: {}", rowsDeleted, fileId);
            
        } catch (SQLException e) {
            logger.error("✗ Failed to delete chunks: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.DELETE_FAILED,
                "chunk_metadata",
                e
            );
            
        } finally {
            closeResources(stmt, conn);
        }
    }
    
    /**
     * Delete specific chunk
     * 
     * Deletes a single chunk metadata record.
     * Also deletes physical chunk from storage (manual cleanup required).
     * 
     * @param fileId File identifier (UUID)
     * @param chunkIndex Zero-based chunk number
     * @throws DatabaseException if delete fails
     */
    public void deleteByFileIdAndIndex(String fileId, Integer chunkIndex) 
            throws DatabaseException {
        logger.debug("Deleting chunk: {}[{}]", fileId, chunkIndex);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(DELETE_BY_FILE_AND_INDEX_QUERY);
            
            // Set parameters
            stmt.setString(1, fileId);
            stmt.setInt(2, chunkIndex);
            
            // Execute delete
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted > 0) {
                logger.info("✓ Chunk deleted: {}[{}]", fileId, chunkIndex);
            } else {
                logger.warn("Chunk not found: {}[{}]", fileId, chunkIndex);
            }
            
        } catch (SQLException e) {
            logger.error("✗ Failed to delete chunk: {}", e.getMessage(), e);
            throw new DatabaseException(
                DatabaseException.ErrorType.DELETE_FAILED,
                "chunk_metadata",
                e
            );
            
        } finally {
            closeResources(stmt, conn);
        }
    }
    
    /**
     * Map ResultSet row to ChunkMetadata object
     * 
     * Converts database row to domain object.
     * 
     * @param rs ResultSet positioned at data row
     * @return ChunkMetadata object
     * @throws SQLException if column access fails
     */
    private ChunkMetadata mapResultSetToChunkMetadata(ResultSet rs) throws SQLException {
        LocalDateTime uploadTimestamp = null;
        Timestamp dbTimestamp = rs.getTimestamp("uploadTimestamp");
        if (dbTimestamp != null) {
            uploadTimestamp = dbTimestamp.toLocalDateTime();
        }
        
        return new ChunkMetadata(
            rs.getInt("id"),
            rs.getString("fileId"),
            rs.getInt("chunkIndex"),
            rs.getString("serverHost"),
            rs.getString("remotePath"),
            rs.getLong("crc32Checksum"),
            rs.getLong("sizeBytes"),
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
