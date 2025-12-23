package com.ntu.cloudgui.aggservice.controller;

import com.ntu.cloudgui.aggservice.dto.ApiResponse;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import com.ntu.cloudgui.aggservice.service.FileProcessingService;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * FileController - HTTP Request Handler for file operations.
 *
 * Endpoints:
 * - POST /files/upload                - Upload file
 * - GET /files/{fileId}/download      - Download file
 * - GET /health                       - Health check
 */
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;  // 5GB
    private static final int BUFFER_SIZE = 8192;

    private final FileProcessingService fileProcessingService;

    public FileController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    /**
     * Handle POST /files/upload
     * Header: X-File-Name (required)
     * Body: Binary file data
     */
    public void handleUpload(HttpExchange exchange) throws IOException {
        logger.info("File upload request received");

        try {
            // Extract and validate filename
            String filename = exchange.getRequestHeaders().getFirst("X-File-Name");

            if (filename == null || filename.isEmpty()) {
                logger.warn("Upload rejected: Missing X-File-Name header");
                sendError(exchange, 400, "Missing X-File-Name header");
                return;
            }

            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                logger.warn("Upload rejected: Invalid filename: {}", filename);
                sendError(exchange, 400, "Invalid filename - path traversal not allowed");
                return;
            }

            // Read file data
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

            logger.info("File data received: {} bytes", fileData.length);

            // Write byte array to temporary file for processing
            File tempFile = createTempFile(filename, fileData);

            try {
                logger.debug("Processing file: {}", filename);
                String fileId = fileProcessingService.processUpload(tempFile, "AES");

                logger.info("✓ File uploaded successfully. FileId: {}", fileId);
                sendSuccess(exchange, 201, new ApiResponse(true, "File uploaded successfully", fileId));

            } finally {
                if (!tempFile.delete()) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }

        } catch (ProcessingException e) {
            logger.warn("Upload processing failed: {}", e.getMessage());
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during upload", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Handle GET /files/{fileId}/download
     */
    public void handleDownload(HttpExchange exchange) throws IOException {
        logger.info("File download request received");

        try {
            // Extract fileId from path
            String path = exchange.getRequestURI().getPath();
            String fileId = extractFileIdFromPath(path);

            if (fileId == null || fileId.isEmpty()) {
                logger.warn("Download rejected: Invalid URL path: {}", path);
                sendError(exchange, 400, "Invalid URL format");
                return;
            }

            logger.debug("Download requested for fileId: {}", fileId);

            // Get file metadata
            logger.debug("Retrieving file: {}", fileId);
            FileMetadata fileMetadata = fileProcessingService.getFile(fileId);

            logger.info("✓ File metadata retrieved. Size: {} bytes", fileMetadata.getSizeBytes());

            // TODO: Implement file reconstruction from chunks
            // For now, return placeholder response
            byte[] fileData = new byte[0];  // Replace with actual reconstruction logic

            // Stream response
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Disposition", 
                    "attachment; filename=\"" + fileMetadata.getOriginalName() + "\"");
            exchange.sendResponseHeaders(200, fileData.length);

            OutputStream os = exchange.getResponseBody();
            os.write(fileData);
            os.close();

            logger.info("✓ File downloaded successfully");

        } catch (ProcessingException e) {
            logger.error("File retrieval failed: {}", e.getMessage(), e);
            sendError(exchange, 404, "File not found or corrupted");
        } catch (Exception e) {
            logger.error("Unexpected error during download", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Handle GET /health
     */
    public void handleHealth(HttpExchange exchange) throws IOException {
        logger.debug("Health check request");

        try {
            sendSuccess(exchange, 200, new ApiResponse(true, "Service is healthy", null));
            logger.debug("✓ Health check passed");
        } catch (Exception e) {
            logger.error("Health check failed", e);
            sendError(exchange, 500, "Service unhealthy");
        }
    }

    /**
     * Read entire request body into byte array.
     */
    private byte[] readRequestBody(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = requestBody.read(buffer)) > 0) {
            baos.write(buffer, 0, bytesRead);
        }

        return baos.toByteArray();
    }

    /**
     * Create temporary file from byte array.
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
     * Expected format: /files/{fileId}/download
     */
    private String extractFileIdFromPath(String path) {
        try {
            String[] parts = path.split("/");

            if (parts.length < 4) {
                return null;
            }

            // parts[0] = "" (empty before leading /)
            // parts[1] = "files"
            // parts[2] = fileId
            // parts[3] = "download"

            String fileId = parts[2];

            // Validate UUID format
            UUID.fromString(fileId);

            return fileId;

        } catch (Exception e) {
            logger.debug("Failed to extract fileId from path: {}", path);
            return null;
        }
    }

    /**
     * Send JSON success response.
     */
    private void sendSuccess(HttpExchange exchange, int statusCode, ApiResponse response)
            throws IOException {
        String responseJson = response.toJson();
        byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(responseBytes.length));
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    /**
     * Send JSON error response.
     */
    private void sendError(HttpExchange exchange, int statusCode, String message)
            throws IOException {
        ApiResponse response = new ApiResponse(false, message, null);
        String responseJson = response.toJson();
        byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(responseBytes.length));
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
