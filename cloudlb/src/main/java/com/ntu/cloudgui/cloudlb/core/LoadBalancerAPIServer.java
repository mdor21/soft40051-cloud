package com.ntu.cloudgui.cloudlb.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Load Balancer HTTP API Server
 * 
 * REST API Endpoints:
 * - POST /api/upload    → Handle file upload requests
 * - POST /api/download  → Handle file download requests
 * - GET  /api/status    → Return system health and node status
 * 
 * Protocol: HTTP/1.1 over TCP/IP (port 8080)
 * Message Format: JSON
 * 
 * CONNECTIVITY:
 * JavaFX GUI (LoadBalancerClient)
 *     ↓ (TCP/IP JSON)
 * LoadBalancerAPIServer (port 8080) ← THIS CLASS
 *     ↓
 * RequestQueue (thread-safe queue)
 *     ↓
 * LoadBalancerWorker (scheduler + routing)
 *     ↓
 * Aggregator Service (internal API)
 */
public class LoadBalancerAPIServer {
    
    private final RequestQueue requestQueue;
    private final int port;
    private HttpServer httpServer;
    private final Gson gson = new Gson();
    
    /**
     * Constructor
     * 
     * @param requestQueue Thread-safe request queue
     * @param port HTTP server port (typically 8080)
     */
    public LoadBalancerAPIServer(RequestQueue requestQueue, int port) {
        this.requestQueue = requestQueue;
        this.port = port;
    }
    
    /**
     * Start the HTTP API server
     * 
     * @throws IOException on server startup error
     */
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 50);
        
        // Register API endpoints
        httpServer.createContext("/api/upload", new UploadHandler());
        httpServer.createContext("/api/download", new DownloadHandler());
        httpServer.createContext("/api/status", new StatusHandler());
        httpServer.createContext("/health", new HealthCheckHandler());
        
        // Set executor and start
        httpServer.setExecutor(Executors.newFixedThreadPool(10));
        httpServer.start();
        
        System.out.printf("[LoadBalancerAPIServer] Started on port %d%n", port);
    }
    
    /**
     * Stop the HTTP API server
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            System.out.println("[LoadBalancerAPIServer] Stopped");
        }
    }
    
    /**
     * POST /api/upload - Handle file upload requests
     * 
     * Request JSON:
     * {
     *   "action": "UPLOAD",
     *   "filename": "test.txt",
     *   "size": 1024,
     *   "checksum": "A1B2C3D4"
     * }
     * 
     * Response JSON:
     * {
     *   "status": "SUCCESS|FAILURE",
     *   "message": "Upload complete",
     *   "fileId": "uuid-12345",
     *   "timestamp": 1699564800000
     * }
     */
    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                String requestBody = readRequestBody(exchange);
                JsonObject request = gson.fromJson(requestBody, JsonObject.class);
                
                // Validate request
                if (!request.has("filename") || !request.has("size")) {
                    sendError(exchange, 400, "Missing required fields: filename, size");
                    return;
                }
                
                String filename = request.get("filename").getAsString();
                long fileSize = request.get("size").getAsLong();
                String checksum = request.has("checksum") ? 
                    request.get("checksum").getAsString() : "";
                
                // Validate file size (max 500MB)
                if (fileSize > 500 * 1024 * 1024) {
                    sendError(exchange, 413, "File too large (max 500MB)");
                    return;
                }
                
                // Create upload request and enqueue
                UploadRequest uploadReq = new UploadRequest(filename, fileSize, checksum);
                requestQueue.enqueue(uploadReq);
                
                System.out.printf("[UploadHandler] Enqueued: %s (size: %d)%n", filename, fileSize);
                
                // Send response
                JsonObject response = new JsonObject();
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Upload request queued");
                response.addProperty("fileId", uploadReq.getRequestId());
                response.addProperty("timestamp", System.currentTimeMillis());
                
                sendResponse(exchange, 200, response.toString());
                
            } catch (Exception e) {
                System.err.printf("[UploadHandler] Error: %s%n", e.getMessage());
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }
    
    /**
     * POST /api/download - Handle file download requests
     * 
     * Request JSON:
     * {
     *   "action": "DOWNLOAD",
     *   "filename": "test.txt"
     * }
     * 
     * Response JSON:
     * {
     *   "status": "SUCCESS|FAILURE",
     *   "message": "File ready for download",
     *   "fileSize": 1024,
     *   "checksum": "A1B2C3D4"
     * }
     */
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                String requestBody = readRequestBody(exchange);
                JsonObject request = gson.fromJson(requestBody, JsonObject.class);
                
                // Validate request
                if (!request.has("filename")) {
                    sendError(exchange, 400, "Missing required field: filename");
                    return;
                }
                
                String filename = request.get("filename").getAsString();
                
                // Create download request and enqueue
                DownloadRequest downloadReq = new DownloadRequest(filename);
                requestQueue.enqueue(downloadReq);
                
                System.out.printf("[DownloadHandler] Enqueued: %s%n", filename);
                
                // Send response
                JsonObject response = new JsonObject();
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Download request queued");
                response.addProperty("fileId", downloadReq.getRequestId());
                response.addProperty("timestamp", System.currentTimeMillis());
                
                sendResponse(exchange, 200, response.toString());
                
            } catch (Exception e) {
                System.err.printf("[DownloadHandler] Error: %s%n", e.getMessage());
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }
    
    /**
     * GET /api/status - Return Load Balancer system status
     * 
     * Response JSON:
     * {
     *   "status": "OK",
     *   "activeNodes": 3,
     *   "totalNodes": 5,
     *   "queueSize": 12,
     *   "schedulerType": "ROUNDROBIN",
     *   "healthyNodes": 3,
     *   "unhealthyNodes": 2,
     *   "timestamp": 1699564800000
     * }
     */
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Get status from NodeRegistry and RequestQueue
                JsonObject response = new JsonObject();
                response.addProperty("status", "OK");
                response.addProperty("activeNodes", 3);  // TODO: Get from NodeRegistry
                response.addProperty("totalNodes", 5);   // TODO: Get from NodeRegistry
                response.addProperty("queueSize", requestQueue.size());
                response.addProperty("schedulerType", "ROUNDROBIN");  // TODO: Get from Scheduler
                response.addProperty("healthyNodes", 3);  // TODO: Get from HealthChecker
                response.addProperty("unhealthyNodes", 2); // TODO: Get from HealthChecker
                response.addProperty("timestamp", System.currentTimeMillis());
                
                System.out.printf("[StatusHandler] Queue size: %d, Active nodes: 3/5%n", 
                    requestQueue.size());
                
                sendResponse(exchange, 200, response.toString());
                
            } catch (Exception e) {
                System.err.printf("[StatusHandler] Error: %s%n", e.getMessage());
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }
    
    /**
     * GET /health - Simple health check (for Docker health checks)
     * 
     * Response: Plain text "OK"
     */
    private class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                sendResponse(exchange, 200, "OK");
            } catch (IOException e) {
                sendError(exchange, 500, "Health check failed");
            }
        }
    }
    
    /**
     * Read JSON request body from HTTP exchange
     * 
     * @param exchange HTTP exchange
     * @return Request body as string
     * @throws IOException on read error
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }
    
    /**
     * Send successful HTTP response
     * 
     * @param exchange HTTP exchange
     * @param statusCode HTTP status code (200, 201, etc.)
     * @param responseBody Response body as string
     * @throws IOException on write error
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) 
            throws IOException {
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    /**
     * Send error HTTP response
     * 
     * @param exchange HTTP exchange
     * @param statusCode HTTP error status code (400, 404, 500, etc.)
     * @param errorMessage Error message
     * @throws IOException on write error
     */
    private void sendError(HttpExchange exchange, int statusCode, String errorMessage) 
            throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("status", "ERROR");
        error.addProperty("message", errorMessage);
        error.addProperty("timestamp", System.currentTimeMillis());
        
        String response = error.toString();
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * Upload Request DTO
     */
    public static class UploadRequest {
        private final String requestId;
        private final String filename;
        private final long fileSize;
        private final String checksum;
        private final long timestamp;
        
        public UploadRequest(String filename, long fileSize, String checksum) {
            this.requestId = java.util.UUID.randomUUID().toString();
            this.filename = filename;
            this.fileSize = fileSize;
            this.checksum = checksum;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getRequestId() { return requestId; }
        public String getFilename() { return filename; }
        public long getFileSize() { return fileSize; }
        public String getChecksum() { return checksum; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Download Request DTO
     */
    public static class DownloadRequest {
        private final String requestId;
        private final String filename;
        private final long timestamp;
        
        public DownloadRequest(String filename) {
            this.requestId = java.util.UUID.randomUUID().toString();
            this.filename = filename;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getRequestId() { return requestId; }
        public String getFilename() { return filename; }
        public long getTimestamp() { return timestamp; }
    }
}
