package com.ntu.cloudgui.aggservice.controller;

import com.ntu.cloudgui.aggservice.dto.ApiResponse;
import com.ntu.cloudgui.aggservice.exception.FileProcessingException;
import com.ntu.cloudgui.aggservice.exception.ValidationException;
import com.ntu.cloudgui.aggservice.service.FileProcessingService;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * FileController - HTTP Request Handler
 * 
 * Handles HTTP requests for file upload and download operations.
 * Provides REST endpoints for the AggService microservice.
 * 
 * Endpoints:
 * - POST /files/upload       - Upload file
 * - GET /files/{fileId}/download - Download file
 * - GET /health             - Health check
 * 
 * Responsibilities:
 * - Parse HTTP requests and headers
 * - Validate input
 * - Delegate to FileProcessingService
 * - Return formatted HTTP responses
 * - Handle errors gracefully
 */
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    private final FileProcessingService fileProcessingService;
    
    // Configuration Constants
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Constructor - Initialize file controller
     * 
     * @param fileProcessingService Service for file processing
     */
    public FileController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }
    
    /**
     * Handle file upload request
     * 
     * Endpoint: POST /files/upload
     * Header: X-File-Name (required) - Original filename
     * Body: Binary file data
     * 
     * Process:
     * 1. Validate filename from header
     * 2. Read file data from request body
     * 3. Encrypt and chunk file
     * 4. Store chunks to file servers
     * 5. Save metadata to database
     * 6. Return fileId in response
     * 
     * @param exchange HTTP exchange object
     * @throws IOException if I/O error occurs
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
            
            // Validate filename - prevent path traversal attacks
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                logger.warn("Upload rejected: Invalid filename: {}", filename);
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
            
            // Validate file size
            if (fileData.length > MAX_FILE_SIZE) {
                logger.warn("Upload rejected: File too large - {} bytes", fileData.length);
                sendError(exchange, 413, "File exceeds maximum size of 5GB");
                return;
            }
            
            logger.info("File data received: {} bytes", fileData.length);
            
            // 3. Process file (encrypt, chunk, store)
            logger.debug("Processing file: {}", filename);
            String fileId = fileProcessingService.encryptAndChunk(filename, fileData);
            
            logger.info("✓ File uploaded successfully. FileId: {}", fileId);
            
            // 4. Send success response
            sendSuccess(exchange, 201, new ApiResponse(
                true,
                "File uploaded successfully",
                fileId
            ));
            
        } catch (ValidationException e) {
            logger.warn("Upload validation failed: {}", e.getMessage());
            sendError(exchange, 400, e.getMessage());
            
        } catch (FileProcessingException e) {
            logger.error("File processing failed: {}", e.getMessage(), e);
            sendError(exchange, 500, "File processing failed: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error("Unexpected error during upload", e);
            sendError(exchange, 500, "Internal server error");
        }
    }
    
    /**
     * Handle file download request
     * 
     * Endpoint: GET /files/{fileId}/download
     * URL Path: /files/{fileId}/download
     * 
     * Process:
     * 1. Extract fileId from URL path
     * 2. Validate fileId format
     * 3. Fetch encrypted chunks from file servers
     * 4. Validate checksums
     * 5. Decrypt and reassemble file
     * 6. Stream file in response
     * 
     * @param exchange HTTP exchange object
     * @throws IOException if I/O error occurs
     */
    public void handleDownload(HttpExchange exchange) throws IOException {
        logger.info("File download request received");
        
        try {
            // 1. Extract fileId from URL path
            String path = exchange.getRequestURI().getPath();
            String fileId = extractFileIdFromPath(path);
            
            if (fileId == null || fileId.isEmpty()) {
                logger.warn("Download rejected: Invalid URL path: {}", path);
                sendError(exchange, 400, "Invalid URL format");
                return;
            }
            
            logger.debug("Download requested for fileId: {}", fileId);
            
            // 2. Reconstruct and decrypt file
            logger.debug("Reconstructing file: {}", fileId);
            byte[] fileData = fileProcessingService.reconstructAndDecrypt(fileId);
            
            logger.info("✓ File reconstructed. Size: {} bytes", fileData.length);
            
            // 3. Stream file in response
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Disposition", 
                                             "attachment; filename=\"download\"");
            exchange.sendResponseHeaders(200, fileData.length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(fileData);
            os.close();
            
            logger.info("✓ File downloaded successfully");
            
        } catch (ValidationException e) {
            logger.warn("Download validation failed: {}", e.getMessage());
            sendError(exchange, 400, e.getMessage());
            
        } catch (FileProcessingException e) {
            logger.error("File reconstruction failed: {}", e.getMessage(), e);
            sendError(exchange, 404, "File not found or corrupted");
            
        } catch (Exception e) {
            logger.error("Unexpected error during download", e);
            sendError(exchange, 500, "Internal server error");
        }
    }
    
    /**
     * Handle health check request
     * 
     * Endpoint: GET /health
     * Response: JSON with status
     * 
     * @param exchange HTTP exchange object
     * @throws IOException if I/O error occurs
     */
    public void handleHealth(HttpExchange exchange) throws IOException {
        logger.debug("Health check request");
        
        try {
            ApiResponse response = new ApiResponse(
                true,
                "Service is healthy",
                null
            );
            
            sendSuccess(exchange, 200, response);
            logger.debug("✓ Health check passed");
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            sendError(exchange, 500, "Service unhealthy");
        }
    }
    
    /**
     * Read entire request body into byte array
     * 
     * Reads from HTTP request input stream with buffer.
     * 
     * @param exchange HTTP exchange object
     * @return Byte array of request body
     * @throws IOException if read fails
     */
    private byte[] readRequestBody(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        
        // Use ByteArrayOutputStream for dynamic sizing
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        
        while ((bytesRead = requestBody.read(buffer)) > 0) {
            baos.write(buffer, 0, bytesRead);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Extract fileId from URL path
     * 
     * Expected format: /files/{fileId}/download
     * 
     * @param path Request path
     * @return FileId or null if invalid
     */
    private String extractFileIdFromPath(String path) {
        try {
            // Path format: /files/{fileId}/download
            String[] parts = path.split("/");
            
            if (parts.length < 4) {
                return null;
            }
            
            // parts = "" (empty before leading /)
            // parts = "files"
            // parts = fileId
            // parts = "download"
            
            String fileId = parts;
            
            // Validate UUID format
            UUID.fromString(fileId);
            
            return fileId;
            
        } catch (Exception e) {
            logger.debug("Failed to extract fileId from path: {}", path);
            return null;
        }
    }
    
    /**
     * Send success response (JSON)
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
        
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    /**
     * Send error response (JSON)
     * 
     * @param exchange HTTP exchange object
     * @param statusCode HTTP status code
     * @param message Error message
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
        
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
