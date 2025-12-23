package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.config.SftpConfig;
import com.ntu.cloudgui.aggservice.config.SftpConnectionPool;
import com.ntu.cloudgui.aggservice.config.SftpConnection;
import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChunkStorageService - Remote Chunk Storage Management.
 *
 * Manages storage of encrypted chunks on remote SFTP servers.
 */
public class ChunkStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkStorageService.class);

    private static final String CHUNK_ROOT = "/data/chunks";
    private static final int MAX_RETRIES = 3;

    private final SftpConfig sftpConfig;
    private final SftpConnectionPool connectionPool;
    private final AtomicInteger roundRobinCounter;

    public ChunkStorageService(SftpConfig sftpConfig, SftpConnectionPool connectionPool) {
        this.sftpConfig = sftpConfig;
        this.connectionPool = connectionPool;
        this.roundRobinCounter = new AtomicInteger(0);

        List<String> servers = getServerList();
        logger.info("ChunkStorageService initialized with {} servers", servers.size());
    }

    /**
     * Select server for storage using round-robin.
     */
    public String selectServer() throws ProcessingException {
        List<String> servers = getServerList();

        if (servers.isEmpty()) {
            throw new ProcessingException("No storage servers available", ErrorType.STORAGE_ERROR);
        }

        int index = Math.abs(roundRobinCounter.getAndIncrement()) % servers.size();
        String server = servers.get(index);

        logger.debug("Server selected (round-robin): {} (index {})", server, index);
        return server;
    }

    /**
     * Store encrypted chunk on remote server.
     */
    public String storeChunk(String serverHost,
                             String fileId,
                             int chunkIndex,
                             byte[] encryptedData) throws ProcessingException {

        logger.info("Storing chunk: {}[{}] to {} ({} bytes)",
                fileId, chunkIndex, serverHost, encryptedData.length);

        String remotePath = buildChunkPath(fileId, chunkIndex);

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
                    logger.warn("Upload attempt {} failed, retrying: {}", attempt, e.getMessage());
                    try {
                        Thread.sleep(Math.min(1000L * attempt, 5000L));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProcessingException("Upload interrupted",
                                ErrorType.STORAGE_ERROR, ie);
                    }
                }
            }
        }

        logger.error("✗ Failed to store chunk after {} attempts: {}",
                MAX_RETRIES, lastException != null ? lastException.getMessage() : "unknown");

        throw lastException != null
                ? lastException
                : new ProcessingException("Failed to store chunk", ErrorType.STORAGE_ERROR);
    }

    /**
     * Retrieve encrypted chunk from remote server.
     */
    public byte[] retrieveChunk(String serverHost, String remotePath) throws ProcessingException {
        logger.info("Retrieving chunk: {} from {}", remotePath, serverHost);

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
                    logger.warn("Download attempt {} failed, retrying: {}", attempt, e.getMessage());
                    try {
                        Thread.sleep(Math.min(1000L * attempt, 5000L));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProcessingException("Download interrupted",
                                ErrorType.STORAGE_ERROR, ie);
                    }
                }
            }
        }

        logger.error("✗ Failed to retrieve chunk after {} attempts: {}",
                MAX_RETRIES, lastException != null ? lastException.getMessage() : "unknown");

        throw lastException != null
                ? lastException
                : new ProcessingException("Failed to retrieve chunk", ErrorType.STORAGE_ERROR);
    }

    /**
     * Delete chunk from remote server.
     */
    public void deleteChunk(String serverHost, String remotePath) throws ProcessingException {
        logger.info("Deleting chunk: {} from {}", remotePath, serverHost);

        SftpConnection connection = null;
        try {
            connection = connectionPool.getConnection(serverHost);
            connection.deleteFile(remotePath);
            logger.info("✓ Chunk deleted: {}", remotePath);
        } catch (Exception e) {
            logger.error("✗ Failed to delete chunk: {}", e.getMessage(), e);
            throw new ProcessingException("Failed to delete chunk: " + e.getMessage(),
                    ErrorType.STORAGE_ERROR, e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(serverHost, connection);
            }
        }
    }

    /**
     * Internal: upload chunk via SFTP.
     */
    private void uploadChunk(String serverHost, String remotePath, byte[] data)
            throws ProcessingException {

        SftpConnection connection = null;
        try {
            connection = connectionPool.getConnection(serverHost);

            String directory = remotePath.substring(0, remotePath.lastIndexOf('/'));
            try {
                connection.createDirectory(directory);
            } catch (Exception e) {
                logger.debug("Directory creation skipped (may exist): {}", directory);
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            connection.uploadFile(inputStream, remotePath);
        } catch (Exception e) {
            logger.error("✗ SFTP upload failed: {}", e.getMessage(), e);
            throw new ProcessingException("SFTP upload failed: " + e.getMessage(),
                    ErrorType.STORAGE_ERROR, e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(serverHost, connection);
            }
        }
    }

    /**
     * Internal: download chunk via SFTP.
     */
    private byte[] downloadChunk(String serverHost, String remotePath) throws ProcessingException {
        SftpConnection connection = null;
        try {
            connection = connectionPool.getConnection(serverHost);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            connection.downloadFile(remotePath, outputStream);

            byte[] data = outputStream.toByteArray();
            logger.debug("Downloaded {} bytes", data.length);
            return data;
        } catch (Exception e) {
            logger.error("✗ SFTP download failed: {}", e.getMessage(), e);
            throw new ProcessingException("SFTP download failed: " + e.getMessage(),
                    ErrorType.STORAGE_ERROR, e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(serverHost, connection);
            }
        }
    }

    /**
     * Build remote path for chunk.
     */
    private String buildChunkPath(String fileId, int chunkIndex) {
        return String.format("%s/%s/chunk-%d", CHUNK_ROOT, fileId, chunkIndex);
    }

    /**
     * Get chunk root directory.
     */
    public String getChunkRoot() {
        return CHUNK_ROOT;
    }

    /**
     * Helper: convert configured serverHosts (String[]) to List<String>.
     */
    private List<String> getServerList() {
        String[] hosts = sftpConfig.getServerHosts();
        return (hosts == null) ? List.of() : Arrays.asList(hosts);
    }
}
