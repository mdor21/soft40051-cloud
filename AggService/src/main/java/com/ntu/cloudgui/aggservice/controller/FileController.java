package com.ntu.cloudgui.aggservice.controller;

import com.ntu.cloudgui.aggservice.dto.ApiResponse;
import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import com.ntu.cloudgui.aggservice.repository.ChunkMetadataRepository;
import com.ntu.cloudgui.aggservice.service.CrcValidationService;
import com.ntu.cloudgui.aggservice.service.EncryptionService;
import com.ntu.cloudgui.aggservice.service.FileProcessingService;
import com.ntu.cloudgui.aggservice.service.ChunkStorageService;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * FileController - HTTP Request Handler for file operations.
 *
 * Handles file upload, download, and health check endpoints.
 *
 * Endpoints:
 * - POST /files/upload                - Upload and encrypt file
 * - GET /files/{fileId}/download      - Download and decrypt file
 * - GET /health                       - Health check (LB compatible)
 *
 * Security:
 * - Path traversal prevention
 * - File size validation (5GB max)
 * - UUID format validation
 * - Error message sanitization
 */
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;  // 5GB
    private static final int BUFFER_SIZE = 8192;

    private final FileProcessingService fileProcessingService;
    private final ChunkMetadataRepository chunkMetadataRepository;
    private final ChunkStorageService chunkStorageService;
    private final EncryptionService encryptionService;
    private final CrcValidationService crcValidationService;

    public FileController(FileProcessingService fileProcessingService,
                         ChunkMetadataRepository chunkMetadataRepository,
                         ChunkStorageService chunkStorageService,
                         EncryptionService encryptionService,
                         CrcValidationService crcValidationService) {
        this.fileProcessingService = fileProcessingService;
        this.chunkMetadataRepository = chunkMetadataRepository;
        this.chunkStorageService = chunkStorageService;
        this.encryptionService = encryptionService;
        this.crcValidationService = crcValidationService;
    }

    /**
     * Handle POST /files/upload
     * 
     * Uploads file, encrypts, chunks, and stores across servers.
     * 
     * Headers:
     * - X-File-Name (required): Original filename
     * 
     * Body: Binary file data
     * 
     * Response:
     * - 201 Created: { "success": true, "message": "...", "data": fileId }
     * - 400 Bad Request: { "success": false, "message": "...", "data": null }
     * - 413 Payload Too Large
     * - 500 Internal Server Error
     */
    public void handleUpload(HttpExchange exchange) throws IOException {
        logger.info("File upload request received");

        try {
            // 1. Extract and validate filename
            String filename = exchange.getRequestHeaders().getFirst("X-File-Name");

            if (filename == null || filename.isEmpty()) {
                logger.warn("Upload rejected: Missing X-File-Name header");
                sendError(exchange, 400, "Missing X-File-Name header");
                return;
            }

            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                logger.warn("Upload rejected: Invalid filename with path traversal: {}", filename);
                sendError(exchange, 400, "Invalid filename - path traversal not allowed");
                return;
            }

            // 2. Read file data from request body
            logger.debug("Reading file data from request body");
            byte[] fileData = readRequestBody(exchange);

            if (fileData.length == 0) {
                logger.warn("Upload rejected: Empty file");
                sendError(exchange, 400, "File is empty");
                return;
            }

            if (fileData.length > MAX_FILE_SIZE) {
                logger.warn("Upload rejected: File too large - {} bytes", fileData.length);
                sendError(exchange, 413, "File exceeds maximum size of 5GB");
                return;
            }

            logger.info("File data received: {} bytes for file: {}", fileData.length, filename);

            // 3. Create temporary file for processing
            File tempFile = createTempFile(filename, fileData);

            try {
                // 4. Process file (encrypt, chunk, store)
                logger.debug("Processing file: {} ({} bytes)", filename, fileData.length);
                String fileId = fileProcessingService.processUpload(tempFile, "AES");

                logger.info("✓ File uploaded successfully. FileId: {}, Filename: {}, Size: {} bytes",
                        fileId, filename, fileData.length);

                // 5. Send success response
                sendSuccess(exchange, 201,
                        new ApiResponse(true, "File uploaded successfully", fileId));

            } finally {
                // Cleanup temporary file
                if (!tempFile.delete()) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }

        } catch (ProcessingException e) {
            logger.warn("Upload processing failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            sendError(exchange, 400, "File processing failed: " + sanitizeErrorMessage(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during upload", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Handle GET /files/{fileId}/download
     * 
     * Downloads file, reconstructs from chunks, decrypts, and validates.
     * 
     * URL Path: /files/{fileId}/download
     * - fileId: UUID of file to download
     * 
     * Response:
     * - 200 OK: Binary file data
     * - 400 Bad Request: Invalid fileId
     * - 404 Not Found: File not found or corrupted
     * - 500 Internal Server Error
     */
    public void handleDownload(HttpExchange exchange) throws IOException {
        logger.info("File download request received");
        String fileId = null;

        try {
            // 1. Extract and validate fileId from URL path
            String path = exchange.getRequestURI().getPath();
            fileId = extractFileIdFromPath(path);

            if (fileId == null || fileId.isEmpty()) {
                logger.warn("Download rejected: Invalid URL path: {}", path);
                sendError(exchange, 400, "Invalid URL format");
                return;
            }

            logger.debug("Download requested for fileId: {}", fileId);

            // 2. Retrieve file metadata
            logger.debug("Retrieving file metadata: {}", fileId);
            FileMetadata fileMetadata = fileProcessingService.getFile(fileId);

            if (fileMetadata == null) {
                logger.warn("Download rejected: File metadata not found for fileId: {}", fileId);
                sendError(exchange, 404, "File not found");
                return;
            }

            logger.info("✓ File metadata retrieved. FileId: {}, Name: {}, Size: {} bytes",
                    fileId, fileMetadata.getOriginalName(), fileMetadata.getSizeBytes());

            // 3. Reconstruct file from chunks
            logger.debug("Reconstructing file from {} chunks", fileMetadata.getTotalChunks());
            byte[] fileData = reconstructFileFromChunks(fileId, fileMetadata);

            logger.info("✓ File reconstructed successfully. Size: {} bytes", fileData.length);

            // 4. Set response headers BEFORE sendResponseHeaders
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"" + fileMetadata.getOriginalName() + "\"");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileData.length));
            
            exchange.sendResponseHeaders(200, fileData.length);

            // 5. Stream file in response
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileData);
            }

            logger.info("✓ File downloaded successfully: {}", fileId);

        } catch (ProcessingException e) {
            logger.error("File retrieval failed for fileId: {} - {}", fileId, e.getMessage());
            sendError(exchange, 404, "File not found or corrupted");
        } catch (Exception e) {
            logger.error("Unexpected error during download", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Handle GET /health
     * 
     * Health check endpoint compatible with load balancers.
     * 
     * Response:
     * - 200 OK: { "success": true, "message": "Service is healthy", "data": null }
     * - 500 Internal Server Error
     */
    public void handleHealth(HttpExchange exchange) throws IOException {
        logger.debug("Health check request");

        try {
            sendSuccess(exchange, 200, 
                    new ApiResponse(true, "Service is healthy", null));
            logger.debug("✓ Health check passed");
        } catch (Exception e) {
            logger.error("Health check failed", e);
            sendError(exchange, 500, "Service unhealthy");
        }
    }

    /**
     * Reconstruct file from encrypted chunks.
     * 
     * Process:
     * 1. Fetch chunks from storage servers (ordered by index)
     * 2. Decrypt each chunk
     * 3. Validate CRC32 checksum
     * 4. Reassemble chunks in order
     * 5. Validate total size
     * 
     * @param fileId File identifier
     * @param fileMetadata File metadata with total chunks count
     * @return Reassembled plaintext file data
     * @throws ProcessingException if reconstruction fails
     */
    private byte[] reconstructFileFromChunks(String fileId, FileMetadata fileMetadata)
            throws ProcessingException {
        
        try {
            // 1. Fetch all chunks ordered by index
            logger.debug("Fetching chunks from metadata repository");
            List<ChunkMetadata> chunks = chunkMetadataRepository.findByFileIdOrderByChunkIndex(fileId);

            if (chunks == null || chunks.isEmpty()) {
                throw new ProcessingException(
                        "No chunks found for file: " + fileId,
                        ErrorType.FILE_NOT_FOUND
                );
            }

            if (fileMetadata.getTotalChunks() <= 0) {
                throw new ProcessingException(
                        "Invalid total chunks: " + fileMetadata.getTotalChunks(),
                        ErrorType.PROCESSING_ERROR
                );
            }

            if (chunks.size() != fileMetadata.getTotalChunks()) {
                throw new ProcessingException(
                        String.format("Chunk count mismatch: expected %d, found %d",
                                fileMetadata.getTotalChunks(), chunks.size()),
                        ErrorType.PROCESSING_ERROR
                );
            }

            String encryptionAlgo = fileMetadata.getEncryptionAlgo();
            if (encryptionAlgo == null || encryptionAlgo.isEmpty()) {
                throw new ProcessingException(
                        "Invalid encryption algorithm",
                        ErrorType.PROCESSING_ERROR
                );
            }

            // 2. Calculate total size and allocate buffer
            long totalSize = 0;
            for (ChunkMetadata chunk : chunks) {
                long chunkSize = chunk.getOriginalSize();
                if (chunkSize <= 0) {
                    throw new ProcessingException(
                            String.format("Invalid chunk size for chunk %d: %d",
                                    chunk.getChunkIndex(), chunkSize),
                            ErrorType.PROCESSING_ERROR
                    );
                }
                totalSize += chunkSize;
            }

            if (totalSize > MAX_FILE_SIZE) {
                throw new ProcessingException(
                        String.format("Reconstructed file exceeds maximum size: %d bytes",
                                totalSize),
                        ErrorType.PROCESSING_ERROR
                );
            }

            byte[] reassembledData = new byte[(int) totalSize];
            int offset = 0;

            logger.debug("Reconstructing {} chunks (total {} bytes)", chunks.size(), totalSize);

            // 3. Process each chunk
            for (ChunkMetadata chunk : chunks) {
                logger.debug("Processing chunk: {}[{}]", fileId, chunk.getChunkIndex());

                // 3a. Fetch encrypted chunk from storage
                byte[] encryptedChunkData = chunkStorageService.retrieveChunk(
                        chunk.getServerHost(),
                        chunk.getRemotePath()
                );

                if (encryptedChunkData == null || encryptedChunkData.length == 0) {
                    throw new ProcessingException(
                            String.format("Empty encrypted data for chunk %d",
                                    chunk.getChunkIndex()),
                            ErrorType.PROCESSING_ERROR
                    );
                }

                // 3b. Decrypt chunk
                byte[] decryptedChunkData = encryptionService.decrypt(
                        encryptedChunkData,
                        encryptionAlgo
                );

                if (decryptedChunkData == null || decryptedChunkData.length == 0) {
                    throw new ProcessingException(
                            String.format("Empty decrypted data for chunk %d",
                                    chunk.getChunkIndex()),
                            ErrorType.PROCESSING_ERROR
                    );
                }

                // 3c. Validate CRC32 checksum
                long calculatedCrc = crcValidationService.calculateCrc32(decryptedChunkData);
                if (calculatedCrc != chunk.getCrc32()) {
                    throw new ProcessingException(
                            String.format("CRC mismatch for chunk %d: expected %d, got %d",
                                    chunk.getChunkIndex(), chunk.getCrc32(), calculatedCrc),
                            ErrorType.PROCESSING_ERROR
                    );
                }

                logger.debug("✓ Chunk validated: {}[{}] CRC={}", 
                        fileId, chunk.getChunkIndex(), calculatedCrc);

                // 3d. Reassemble chunk into buffer
                System.arraycopy(
                        decryptedChunkData, 0,
                        reassembledData, offset,
                        decryptedChunkData.length
                );

                offset += decryptedChunkData.length;
            }

            // 4. Validate total size
            if (offset != totalSize) {
                throw new ProcessingException(
                        String.format("Size mismatch: expected %d bytes, got %d bytes",
                                totalSize, offset),
                        ErrorType.PROCESSING_ERROR
                );
            }

            logger.debug("✓ All chunks reconstructed and validated: {} bytes", offset);
            return reassembledData;

        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Chunk reconstruction failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "Failed to reconstruct file from chunks: " + e.getMessage(),
                    ErrorType.PROCESSING_ERROR,
                    e
            );
        }
    }

    /**
     * Read entire request body into byte array.
     * 
     * @param exchange HTTP exchange object
     * @return Byte array of request body
     * @throws IOException if read fails
     */
    private byte[] readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = requestBody.read(buffer)) > 0) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }

    /**
     * Create temporary file from byte array.
     * 
     * @param filename Original filename
     * @param data File data
     * @return Temporary File object
     * @throws IOException if file creation fails
     */
    private File createTempFile(String filename, byte[] data) throws IOException {
        File tempFile = File.createTempFile("upload_", "_" + filename);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
        }
        logger.debug("Temporary file created: {}", tempFile.getAbsolutePath());
        return tempFile;
    }

    /**
     * Extract fileId from URL path.
     * 
     * Expected format: /files/{fileId}/download
     * 
     * @param path Request path
     * @return FileId (UUID string) or null if invalid
     */
    private String extractFileIdFromPath(String path) {
        try {
            String[] parts = path.split("/");

            // Expected: ["", "files", "{fileId}", "download"]
            if (parts.length < 4) {
                logger.debug("Invalid path structure: {}", path);
                return null;
            }

            String fileId = parts[2];

            // Validate UUID format
            UUID.fromString(fileId);

            logger.debug("Extracted fileId: {}", fileId);
            return fileId;

        } catch (IllegalArgumentException e) {
            logger.debug("Invalid UUID format in path: {}", path);
            return null;
        } catch (Exception e) {
            logger.debug("Failed to extract fileId from path: {}", path);
            return null;
        }
    }

    /**
     * Send JSON success response.
     * 
     * @param exchange HTTP exchange object
     * @param statusCode HTTP status code
     * @param response ApiResponse object
     * @throws IOException if write fails
     */
    private void sendSuccess(HttpExchange exchange, int statusCode, ApiResponse response)
            throws IOException {
        String responseJson = response.toJson();
        byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(responseBytes.length));
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Send JSON error response.
     * 
     * @param exchange HTTP exchange object
     * @param statusCode HTTP status code
     * @param message Error message (sanitized)
     * @throws IOException if write fails
     */
    private void sendError(HttpExchange exchange, int statusCode, String message)
            throws IOException {
        ApiResponse response = new ApiResponse(false, message, null);
        String responseJson = response.toJson();
        byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(responseBytes.length));
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Sanitize error message to avoid leaking internal details to client.
     * 
     * @param message Original error message
     * @return Sanitized message safe for client display
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "An error occurred";
        }
        // Remove stack traces, file paths, internal details
        return message.split("\n")[0]  // First line only
                .replaceAll("\\[.*\\]", "")  // Remove brackets
                .replaceAll("/.*\\.java.*", "")  // Remove file paths
                .trim();
    }
}
