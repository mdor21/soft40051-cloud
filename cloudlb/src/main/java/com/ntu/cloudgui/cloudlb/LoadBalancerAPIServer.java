package com.ntu.cloudgui.cloudlb;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private final int port;
    private HttpServer httpServer;
    private final HttpClient httpClient;

    /**
     * Create a new LoadBalancer API Server.
     *
     * @param requestQueue RequestQueue to queue file operations
     * @param port HTTP port to listen on (typically 8080)
     */
    public LoadBalancerAPIServer(RequestQueue requestQueue, int port) {
        this.requestQueue = requestQueue;
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

                if (fileName == null || fileSizeStr == null) {
                    sendError(exchange, 400, "Missing file metadata headers");
                    return;
                }

                long fileSize = parseFileSize(fileSizeStr);
                String fileId = UUID.randomUUID().toString();

                // Read file content from request body
                byte[] fileContent = exchange.getRequestBody().readAllBytes();
                System.out.printf("[API] UPLOAD %s: %s (%.2f MB)%n",
                        fileId, fileName, fileContent.length / 1_000_000.0);

                // Forward to Aggregator service
                forwardUploadToAggregator(fileId, fileName, fileContent);

                // Queue request for load balancer
                Request request = new Request(fileId, Request.Type.UPLOAD, fileSize, 0);
                requestQueue.add(request);
                requestQueue.notifyNewRequest();

                // Send success response
                String response = "{\"fileId\":\"" + fileId
                        + "\",\"status\":\"queued\",\"fileName\":\"" + fileName
                        + "\",\"size\":" + fileSize + "}";

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
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

                // Fetch file from Aggregator
                byte[] fileContent = fetchFileFromAggregator(fileId);

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

                String response = "{\"status\":\"" + status
                        + "\",\"queue_size\":" + queueSize + "}";

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();

                System.out.println("[API] HEALTH CHECK: " + status + " | Queue: " + queueSize);

            } catch (Exception e) {
                System.err.println("[API] Health check error: " + e.getMessage());
                sendError(exchange, 500, "Health check failed");
            }
        }
    }

    /**
     * Forward file upload to Aggregator service.
     *
     * Sends HTTP POST request to first Aggregator (aggservice-1:8080). File
     * content is sent in request body.
     *
     * @param fileId Unique file identifier
     * @param fileName Name of the file
     * @param fileContent File content bytes
     */
    private void forwardUploadToAggregator(String fileId, String fileName, byte[] fileContent) {
        try {
            String aggUrl = "http://aggservice-1:8080/files/upload";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(aggUrl))
                    .header("X-File-ID", fileId)
                    .header("X-File-Name", fileName)
                    .header("X-File-Size", String.valueOf(fileContent.length))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(fileContent))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.printf("[API] Aggregator error (HTTP %d): %s%n",
                        response.statusCode(), response.body());
            } else {
                System.out.println("[API] Forwarded to Aggregator: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("[API] Error forwarding to Aggregator: " + e.getMessage());
        }
    }

    /**
     * Fetch file from Aggregator service.
     *
     * Sends HTTP GET request to Aggregator to retrieve file content.
     *
     * @param fileId File identifier to fetch
     * @return File content bytes
     * @throws Exception if fetch fails
     */
    private byte[] fetchFileFromAggregator(String fileId) throws Exception {
        String aggUrl = "http://aggservice-1:8080/files/" + fileId + "/download";

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
            String errorJson = "{\"error\":\"" + message + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, errorJson.getBytes().length);
            exchange.getResponseBody().write(errorJson.getBytes());
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
