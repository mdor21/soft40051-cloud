package com.ntu.cloudgui.cloudlb;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.ntu.cloudgui.cloudlb.core.NodeRegistry;
import com.ntu.cloudgui.cloudlb.core.Scheduler;
import com.ntu.cloudgui.cloudlb.core.StorageNode;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * LoadBalancer HTTP API Server
 *
 * Exposes REST endpoints for file operations: - POST /api/files/upload - Upload
 * a file to the load balancer - GET /api/files/{fileId}/download - Download a
 * file from the load balancer - GET /api/health - Check load balancer health
 * status
 *
 * All file operations are queued in RequestQueue for processing by
 * LoadBalancerWorker. Files are forwarded to Aggregator services for actual
 * storage.
 *
 * Thread Safety: HttpServer is thread-safe
 */
public class LoadBalancerAPIServer {

    private final RequestQueue requestQueue;
    private final NodeRegistry nodeRegistry;
    private final Scheduler scheduler;
    private final int port;
    private HttpServer httpServer;
    private final HttpClient httpClient;

    /**
     * Create a new LoadBalancer API Server.
     *
     * @param requestQueue RequestQueue to queue file operations
     * @param port HTTP port to listen on (typically 8080)
     */
    public LoadBalancerAPIServer(RequestQueue requestQueue, NodeRegistry nodeRegistry, Scheduler scheduler, int port) {
        this.requestQueue = requestQueue;
        this.nodeRegistry = nodeRegistry;
        this.scheduler = scheduler;
        this.port = port;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Start the HTTP API server.
     *
     * Creates and starts HttpServer with three endpoints: - /api/files/upload
     * (POST) - /api/files/{fileId}/download (GET) - /api/health (GET)
     *
     * @throws IOException if server cannot be started
     */
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Register HTTP handlers
        httpServer.createContext("/api/files/upload", new UploadHandler());
        httpServer.createContext("/api/files", new FileHandler());
        httpServer.createContext("/api/health", new HealthHandler());

        httpServer.setExecutor(null); // Use default executor
        httpServer.start();

        System.out.println("[API Server] Started on port " + port);
    }

    /**
     * Stop the HTTP API server.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            System.out.println("[API Server] Stopped");
        }
    }

    /**
     * Escape special characters in JSON strings.
     * 
     * @param input String to escape
     * @return Escaped string safe for JSON
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    /**
     * Handler for file upload requests.
     *
     * Endpoint: POST /api/files/upload Headers: X-File-Name, X-File-Size Body:
     * File content (bytes)
     *
     * Response: JSON with fileId
     */
    private class UploadHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Extract file metadata from headers
                String fileName = exchange.getRequestHeaders().getFirst("X-File-Name");
                String fileSizeStr = exchange.getRequestHeaders().getFirst("X-File-Size");
                String requestedFileId = exchange.getRequestHeaders().getFirst("X-File-ID");

                if (fileName == null || fileSizeStr == null) {
                    sendError(exchange, 400, "Missing file metadata headers");
                    return;
                }

                long fileSize = parseFileSize(fileSizeStr);
                String fileId = (requestedFileId == null || requestedFileId.isBlank())
                    ? UUID.randomUUID().toString()
                    : requestedFileId;

                // Read file content from request body
                byte[] fileContent = exchange.getRequestBody().readAllBytes();
                System.out.printf("[API] UPLOAD %s: %s (%.2f MB)%n",
                        fileId, fileName, fileContent.length / 1_000_000.0);

                FileBufferStore.put(fileId, fileName, fileContent);

                // Queue request for load balancer
                Request request = new Request(fileId, Request.Type.UPLOAD, fileSize, 0);
                requestQueue.add(request);
                requestQueue.notifyNewRequest();

                // Send success response
                String response = "{\"fileId\":\"" + escapeJson(fileId)
                        + "\",\"status\":\"queued\",\"fileName\":\"" + escapeJson(fileName)
                        + "\",\"size\":" + fileSize + "}";

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                exchange.close();

            } catch (Exception e) {
                System.err.println("[API] Upload error: " + e.getMessage());
                sendError(exchange, 500, "Upload failed: " + e.getMessage());
            }
        }
    }

    /**
     * Handler for file download and operations.
     *
     * Endpoint: GET /api/files/{fileId}/download Response: File content (bytes)
     */
    private class FileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Extract fileId from path: /api/files/{fileId}/download
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");

                if (parts.length < 4 || !parts[3].equals("download")) {
                    sendError(exchange, 400, "Invalid request format");
                    return;
                }

                String fileId = parts[2];
                System.out.println("[API] DOWNLOAD: " + fileId);

                // Queue download request
                Request request = new Request(fileId, Request.Type.DOWNLOAD, 0, 0);
                requestQueue.add(request);
                requestQueue.notifyNewRequest();

                // Synchronous load balancing for download
                List<StorageNode> healthyNodes = nodeRegistry.getHealthyNodes();
                if (healthyNodes.isEmpty()) {
                    sendError(exchange, 503, "No healthy nodes available");
                    return;
                }

                Request downloadRequest = new Request(fileId, Request.Type.DOWNLOAD, 0, 0);
                StorageNode selectedNode = scheduler.selectNode(healthyNodes, downloadRequest);
                if (selectedNode == null) {
                    sendError(exchange, 500, "Scheduler failed to select a node");
                    return;
                }

                // Fetch file from the selected node
                byte[] fileContent = fetchFileFromNode(selectedNode, fileId);

                if (fileContent == null || fileContent.length == 0) {
                    sendError(exchange, 404, "File not found");
                    return;
                }

                // Send file content to client
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileContent.length));
                exchange.sendResponseHeaders(200, fileContent.length);
                exchange.getResponseBody().write(fileContent);
                exchange.close();

                System.out.printf("[API] DOWNLOAD %s: Sent %.2f MB%n",
                        fileId, fileContent.length / 1_000_000.0);

            } catch (Exception e) {
                System.err.println("[API] Download error: " + e.getMessage());
                sendError(exchange, 500, "Download failed: " + e.getMessage());
            }
        }
    }

    /**
     * Handler for health check requests.
     *
     * Endpoint: GET /api/health Response: JSON with status and queue size
     */
    private class HealthHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String status = "HEALTHY";
                int queueSize = requestQueue.size();

                String response = "{\"status\":\"" + escapeJson(status)
                        + "\",\"queue_size\":" + queueSize + "}";

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                exchange.close();

                System.out.println("[API] HEALTH CHECK: " + status + " | Queue: " + queueSize);

            } catch (Exception e) {
                System.err.println("[API] Health check error: " + e.getMessage());
                sendError(exchange, 500, "Health check failed");
            }
        }
    }


    private byte[] fetchFileFromNode(StorageNode node, String fileId) throws Exception {
        String aggUrl = "http://" + node.getAddress() + "/api/files/" + fileId + "/download";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(aggUrl))
                .GET()
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() >= 400) {
            throw new Exception("Aggregator returned HTTP " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Send error response to client.
     *
     * @param exchange HTTP exchange
     * @param statusCode HTTP status code
     * @param message Error message
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) {
        try {
            String errorJson = "{\"error\":\"" + escapeJson(message) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, errorJson.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(errorJson.getBytes(StandardCharsets.UTF_8));
            exchange.close();

            System.err.println("[API] HTTP " + statusCode + ": " + message);
        } catch (IOException e) {
            System.err.println("[API] Error sending error response: " + e.getMessage());
        }
    }

    /**
     * Parse file size from string header.
     *
     * @param fileSizeStr File size as string (from HTTP header)
     * @return File size as long, or 0 if parsing fails
     */
    private long parseFileSize(String fileSizeStr) {
        try {
            return Long.parseLong(fileSizeStr);
        } catch (NumberFormatException e) {
            System.err.println("[API] Invalid file size format: " + fileSizeStr);
            return 0;
        }
    }
}
