package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.config.SftpConfig;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChunkStorageService - Remote Chunk Storage Management
 * 
 * Manages storage of encrypted chunks on remote SFTP servers.
 * 
 * Responsibilities:
 * - Select appropriate server (load balancing)
 * - Store encrypted chunks via SFTP
 * - Retrieve chunks from storage
 * - Delete chunks from storage
 * - Path management and organization
 * 
 * Server Selection Strategy:
 * - Round-robin distribution
 * - Load balancing across available servers
 * - Fallback if server unavailable
 * 
 * Path Organization:
 * /data/chunks/[fileId]/chunk-[index]
 * 
 * Example:
 * <pre>
 * ChunkStorageService storageService = new ChunkStorageService(
 *     sftpConfig, sftpConnectionPool
 * );
 * 
 * // Upload
 * String server = storageService.selectServer();
 * String remotePath = storageService.storeChunk(
 *     server, fileId, chunkIndex, encryptedData
 * );
 * 
 * // Download
 * byte[] encryptedData = storageService.retrieveChunk(server, remotePath);
 * 
 * // Delete
 * storageService.deleteChunk(server, remotePath);
 * </pre>
 */
public class ChunkStorageService {
    private static final Logger logger = LoggerFactory.getLogger(ChunkStorageService.class);
    
    // Configuration
    private static final String CHUNK_ROOT = "/data/chunks";
    private static final int MAX_RETRIES = 3;
    
    private final SftpConfig sftpConfig;
    private final SftpConnectionPool connectionPool;
    private final AtomicInteger roundRobinCounter;
    
    /**
     * Constructor - Initialize storage service
     * 
     * @param sftpConfig SFTP configuration with server list
     * @param connectionPool Connection pool for SFTP connections
     */
    public ChunkStorageService(SftpConfig sftpConfig, SftpConnectionPool connectionPool) {
        this.sftpConfig = sftpConfig;
        this.connectionPool = connectionPool;
        this.roundRobinCounter = new AtomicInteger(0);
        
        logger.info("ChunkStorageService initialized with {} servers", 
                   sftpConfig.getServerHosts().size());
    }
    
    /**
     * Select server for storage using round-robin
     * 
     * Distributes chunks evenly across available servers.
     * Provides basic load balancing.
     * 
     * @return Selected server hostname
     * @throws ProcessingException if no servers available
     */
    public String selectServer() throws ProcessingException {
        List<String> servers = sftpConfig.getServerHosts();
        
        if (servers.isEmpty()) {
            throw new ProcessingException(
                ProcessingException.ErrorType.STORAGE_ERROR,
                "No storage servers available"
            );
        }
        
        // Round-robin selection
        int index = Math.abs(roundRobinCounter.getAndIncrement()) % servers.size();
        String server = servers.get(index);
        
        logger.debug("Server selected (round-robin): {} (index {})", server, index);
        
        return server;
    }
    
    /**
     * Store encrypted chunk on remote server
     * 
     * Uploads encrypted chunk via SFTP with automatic retry.
     * 
     * Path structure: /data/chunks/[fileId]/chunk-[index]
     * 
     * @param serverHost SFTP server hostname
     * @param fileId File identifier
     * @param chunkIndex Zero-based chunk number
     * @param encryptedData Encrypted chunk data
     * @return Remote path where chunk is stored
     * @throws ProcessingException if upload fails
     */
    public String storeChunk(
            String serverHost,
            String fileId,
            int chunkIndex,
            byte[] encryptedData) 
            throws ProcessingException {
        
        logger.info("Storing chunk: {}[{}] to {} ({} bytes)", 
                   fileId, chunkIndex, serverHost, encryptedData.length);
        
        // Build remote path
        String remotePath = buildChunkPath(fileId, chunkIndex);
        
        // Try to upload with retries
        int attempt = 0;
        ProcessingException lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                uploadChunk(serverHost, remotePath, encryptedData);
                logger.info("✓ Chunk stored successfully: {}[{}] @ {}", 
                           fileId, chunkIndex, remotePath);
                
                return remotePath;
                
            } catch (ProcessingException e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    logger.warn("Upload attempt {} failed, retrying: {}", 
                              attempt, e.getMessage());
                    
                    // Exponential backoff
                    try {
                        Thread.sleep(Math.min(1000 * attempt, 5000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProcessingException(
                            ProcessingException.ErrorType.STORAGE_ERROR,
                            "Upload interrupted",
                            ie
                        );
                    }
                }
            }
        }
        
        logger.error("✗ Failed to store chunk after {} attempts: {}", 
                    MAX_RETRIES, lastException.getMessage());
        
        throw lastException;
    }
    
    /**
     * Retrieve encrypted chunk from remote server
     * 
     * Downloads encrypted chunk via SFTP with automatic retry.
     * 
     * @param serverHost SFTP server hostname
     * @param remotePath Remote path of chunk
     * @return Encrypted chunk data
     * @throws ProcessingException if download fails
     */
    public byte[] retrieveChunk(String serverHost, String remotePath) 
            throws ProcessingException {
        
        logger.info("Retrieving chunk: {} from {}", remotePath, serverHost);
        
        // Try to download with retries
        int attempt = 0;
        ProcessingException lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                byte[] chunkData = downloadChunk(serverHost, remotePath);
                logger.info("✓ Chunk retrieved successfully: {} ({} bytes)", 
                           remotePath, chunkData.length);
                
                return chunkData;
                
            } catch (ProcessingException e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    logger.warn("Download attempt {} failed, retrying: {}", 
                              attempt, e.getMessage());
                    
                    // Exponential backoff
                    try {
                        Thread.sleep(Math.min(1000 * attempt, 5000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProcessingException(
                            ProcessingException.ErrorType.STORAGE_ERROR,
                            "Download interrupted",
                            ie
                        );
                    }
                }
            }
        }
        
        logger.error("✗ Failed to retrieve chunk after {} attempts: {}", 
                    MAX_RETRIES, lastException.getMessage());
        
        throw lastException;
    }
    
    /**
     * Delete chunk from remote server
     * 
     * Removes encrypted chunk file from SFTP server.
     * 
     * @param serverHost SFTP server hostname
     * @param remotePath Remote path of chunk
     * @throws ProcessingException if deletion fails
     */
    public void deleteChunk(String serverHost, String remotePath) 
            throws ProcessingException {
        
        logger.info("Deleting chunk: {} from {}", remotePath, serverHost);
        
        // Get connection from pool
        SftpConnection connection = null;
        
        try {
            connection = connectionPool.getConnection(serverHost);
            
            // Delete file
            connection.deleteFile(remotePath);
            logger.info("✓ Chunk deleted: {}", remotePath);
            
        } catch (Exception e) {
            logger.error("✗ Failed to delete chunk: {}", e.getMessage(), e);
            throw new ProcessingException(
                ProcessingException.ErrorType.STORAGE_ERROR,
                "Failed to delete chunk: " + e.getMessage(),
                e
            );
            
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(serverHost, connection);
            }
        }
    }
    
    /**
     * Upload chunk to SFTP server
     * 
     * Internal method - handles actual SFTP upload.
     * 
     * @param serverHost SFTP server hostname
     * @param remotePath Target remote path
     * @param data Chunk data to upload
     * @throws ProcessingException if upload fails
     */
    private void uploadChunk(String serverHost, String remotePath, byte[] data) 
            throws ProcessingException {
        
        SftpConnection connection = null;
        
        try {
            // Get connection from pool
            connection = connectionPool.getConnection(serverHost);
            
            // Create directory if needed
            String directory = remotePath.substring(0, remotePath.lastIndexOf('/'));
            try {
                connection.createDirectory(directory);
            } catch (Exception e) {
                // Directory might already exist
                logger.debug("Directory creation skipped (may exist): {}", directory);
            }
            
            // Upload file
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            connection.uploadFile(inputStream, remotePath);
            
        } catch (Exception e) {
            logger.error("✗ SFTP upload failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                ProcessingException.ErrorType.STORAGE_ERROR,
                "SFTP upload failed: " + e.getMessage(),
                e
            );
            
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(serverHost, connection);
            }
        }
    }
    
    /**
     * Download chunk from SFTP server
     * 
     * Internal method - handles actual SFTP download.
     * 
     * @param serverHost SFTP server hostname
     * @param remotePath Remote path of chunk
     * @return Downloaded chunk data
     * @throws ProcessingException if download fails
     */
    private byte[] downloadChunk(String serverHost, String remotePath) 
            throws ProcessingException {
        
        SftpConnection connection = null;
        
        try {
            // Get connection from pool
            connection = connectionPool.getConnection(serverHost);
            
            // Download file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            connection.downloadFile(remotePath, outputStream);
            
            byte[] data = outputStream.toByteArray();
            logger.debug("Downloaded {} bytes", data.length);
            
            return data;
            
        } catch (Exception e) {
            logger.error("✗ SFTP download failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                ProcessingException.ErrorType.STORAGE_ERROR,
                "SFTP download failed: " + e.getMessage(),
                e
            );
            
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(serverHost, connection);
            }
        }
    }
    
    /**
     * Build remote path for chunk
     * 
     * Constructs: /data/chunks/[fileId]/chunk-[index]
     * 
     * @param fileId File identifier
     * @param chunkIndex Zero-based chunk number
     * @return Remote path
     */
    private String buildChunkPath(String fileId, int chunkIndex) {
        return String.format("%s/%s/chunk-%d", CHUNK_ROOT, fileId, chunkIndex);
    }
    
    /**
     * Get chunk root directory
     * 
     * @return Root path for all chunks
     */
    public String getChunkRoot() {
        return CHUNK_ROOT;
    }
}
