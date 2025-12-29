package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ChunkMetadataRepository - Data Access Layer for Chunk Metadata
 *
 * Provides database operations for chunk metadata persistence.
 * Handles CRUD operations and custom queries for chunk retrieval.
 *
 * Key Operations:
 * - Find chunks by fileId
 * - Find single chunk by fileId and index
 * - Delete chunks by fileId
 * - Count chunks for file
 * - Validate chunk existence
 *
 * Database Table: chunk_metadata
 * Indexes: (fileId, chunkIndex), (fileId), (serverHost)
 */
@Repository
public interface ChunkMetadataRepository extends JpaRepository<ChunkMetadata, Long> {

    /**
     * Find all chunks for a file, ordered by chunk index.
     *
     * Used during file reconstruction to retrieve chunks in correct order.
     *
     * Example:
     * <pre>
     * List<ChunkMetadata> chunks = repository
     *     .findByFileIdOrderByChunkIndex("file-uuid-123");
     * // Returns: [chunk 0, chunk 1, chunk 2, ...]
     * </pre>
     *
     * @param fileId Unique file identifier (UUID)
     * @return List of chunks ordered by index (0, 1, 2, ...)
     *         Empty list if no chunks found
     */
    List<ChunkMetadata> findByFileIdOrderByChunkIndex(String fileId);

    /**
     * Find a single chunk by fileId and chunk index.
     *
     * Used to retrieve specific chunk metadata.
     *
     * Example:
     * <pre>
     * Optional<ChunkMetadata> chunk = repository
     *     .findByFileIdAndChunkIndex("file-uuid-123", 0);
     * if (chunk.isPresent()) {
     *     ChunkMetadata metadata = chunk.get();
     *     // Use metadata to fetch chunk from storage
     * }
     * </pre>
     *
     * @param fileId Unique file identifier (UUID)
     * @param chunkIndex Index of chunk to retrieve (0-based)
     * @return Optional containing chunk metadata if found
     */
    Optional<ChunkMetadata> findByFileIdAndChunkIndex(String fileId, Integer chunkIndex);

    /**
     * Find all chunks stored on a specific server.
     *
     * Used for server-side operations, backups, or cleanup.
     *
     * Example:
     * <pre>
     * List<ChunkMetadata> serverChunks = repository
     *     .findByServerHost("storage-server-1.cloud.local");
     * </pre>
     *
     * @param serverHost Storage server hostname or IP address
     * @return List of chunks on the specified server
     *         Empty list if no chunks found
     */
    List<ChunkMetadata> findByServerHost(String serverHost);

    /**
     * Count total chunks for a file.
     *
     * Used to verify chunk count matches expected value.
     *
     * Example:
     * <pre>
     * long count = repository.countByFileId("file-uuid-123");
     * if (count != expectedTotalChunks) {
     *     throw new IncompleteFileException();
     * }
     * </pre>
     *
     * @param fileId Unique file identifier (UUID)
     * @return Number of chunks for this file
     */
    long countByFileId(String fileId);

    /**
     * Check if a chunk exists.
     *
     * Used before attempting to retrieve a chunk.
     *
     * Example:
     * <pre>
     * boolean exists = repository
     *     .existsByFileIdAndChunkIndex("file-uuid-123", 0);
     * </pre>
     *
     * @param fileId Unique file identifier (UUID)
     * @param chunkIndex Index of chunk to check (0-based)
     * @return true if chunk exists, false otherwise
     */
    boolean existsByFileIdAndChunkIndex(String fileId, Integer chunkIndex);

    /**
     * Delete all chunks for a file.
     *
     * Called during file deletion to clean up metadata.
     * Physical chunks on storage servers must be deleted separately.
     *
     * Example:
     * <pre>
     * repository.deleteByFileId("file-uuid-123");
     * // Metadata deleted, but SFTP cleanup needed separately
     * </pre>
     *
     * @param fileId Unique file identifier (UUID)
     */
    void deleteByFileId(String fileId);

    /**
     * Find chunks by file and validate total count.
     *
     * Custom query with validation.
     * Returns chunks ordered by index for reconstruction.
     *
     * @param fileId Unique file identifier (UUID)
     * @param expectedCount Expected number of chunks
     * @return List of chunks if count matches, empty list otherwise
     */
    @Query("SELECT c FROM ChunkMetadata c WHERE c.fileId = :fileId " +
           "ORDER BY c.chunkIndex ASC")
    List<ChunkMetadata> findChunksForFile(@Param("fileId") String fileId);

    /**
     * Find all chunks on a server, paginated.
     *
     * Used for large-scale server operations with memory efficiency.
     *
     * @param serverHost Storage server hostname or IP address
     * @param offset Starting position (0-based)
     * @param limit Maximum number of results
     * @return List of chunks from specified server (paginated)
     */
    @Query(value = "SELECT * FROM chunk_metadata WHERE serverHost = :serverHost " +
                   "LIMIT :limit OFFSET :offset",
           nativeQuery = true)
    List<ChunkMetadata> findByServerHostPaginated(
            @Param("serverHost") String serverHost,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /**
     * Get all unique server hosts storing chunks.
     *
     * Used for topology discovery and health checks.
     *
     * Example:
     * <pre>
     * List<String> servers = repository.findAllServerHosts();
     * // Returns: ["storage-1", "storage-2", "storage-3"]
     * </pre>
     *
     * @return List of unique server hostnames
     */
    @Query("SELECT DISTINCT c.serverHost FROM ChunkMetadata c")
    List<String> findAllServerHosts();

    /**
     * Get storage size used by all chunks.
     *
     * Calculates total encrypted storage used.
     * Useful for capacity planning.
     *
     * Example:
     * <pre>
     * long totalSize = repository.getTotalStorageSize();
     * System.out.println("Total storage: " + (totalSize / 1024 / 1024 / 1024) + " GB");
     * </pre>
     *
     * @return Sum of all encrypted chunk sizes in bytes
     */
    @Query("SELECT COALESCE(SUM(c.encryptedSize), 0) FROM ChunkMetadata c")
    long getTotalStorageSize();

    /**
     * Get storage size used by chunks on specific server.
     *
     * Used for per-server storage accounting.
     *
     * @param serverHost Storage server hostname or IP address
     * @return Sum of encrypted chunk sizes on this server in bytes
     */
    @Query("SELECT COALESCE(SUM(c.encryptedSize), 0) FROM ChunkMetadata c " +
           "WHERE c.serverHost = :serverHost")
    long getStorageSizeByServer(@Param("serverHost") String serverHost);

    /**
     * Get storage size used by a specific file.
     *
     * Includes overhead from encryption.
     *
     * @param fileId Unique file identifier (UUID)
     * @return Sum of encrypted chunk sizes for this file in bytes
     */
    @Query("SELECT COALESCE(SUM(c.encryptedSize), 0) FROM ChunkMetadata c " +
           "WHERE c.fileId = :fileId")
    long getStorageSizeByFile(@Param("fileId") String fileId);

    /**
     * Verify data consistency for a file.
     *
     * Checks that all expected chunks exist and CRC values are set.
     *
     * @param fileId Unique file identifier (UUID)
     * @param expectedChunkCount Expected total number of chunks
     * @return true if all chunks present and valid, false otherwise
     */
    @Query("SELECT COUNT(c) = :expectedCount FROM ChunkMetadata c " +
           "WHERE c.fileId = :fileId AND c.crc32 IS NOT NULL")
    boolean isFileComplete(@Param("fileId") String fileId,
                          @Param("expectedCount") int expectedChunkCount);
}
