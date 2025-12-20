package com.ntu.cloudgui.cloudlb.api;

import com.ntu.cloudgui.cloudlb.core.Request;
import com.ntu.cloudgui.cloudlb.core.RequestQueue;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * MISSING ENTRY - HTTP Server that exposes the Load Balancer as a REST API.
 * 
 * This server is the entry point for requests from the JavaFX GUI application.
 * 
 * CONNECTIVITY CONFIGURATION:
 * - Bind Address: 0.0.0.0 (all interfaces, accessible via service name 'lb')
 * - Port: 8080 (internal Docker port)
 * - Network: soft40051_network (Docker bridge network)
 * 
 * REQUEST FLOW:
 * 1. User clicks "Upload File" in GUI (FilesController)
 * 2. GUI calls LoadBalancerClient.uploadFile() 
 * 3. Client sends HTTP POST to http://lb:8080/api/files/upload
 * 4. LoadBalancerAPIServer receives request in handleUpload()
 * 5. Creates Request object with UPLOAD type and file size
 * 6. Enqueues into RequestQueue with priority based on file size
 * 7. Returns fileId immediately to client
 * 8. LoadBalancerWorker dequeues and processes asynchronously
 * 9. Worker applies artificial latency (1-5 seconds)
 * 10. Worker routes to Aggregator based on scheduling algorithm
 * 
 * Endpoints:
 * - POST /api/files/upload - Upload file
 * - GET /api/files/{fileId}/download - Download file
 * - GET /api/health - Health check
 */
public class LoadBalancerAPIServer {

    private final RequestQueue requestQueue;
    private final int port;
    private HttpServer server;

    public LoadBalancerAPIServer(RequestQueue requestQueue, int port) {
        this.requestQueue = requestQueue;
        this.port = port;
    }

    /**
     * Start the HTTP server and register request handlers.
     * 
     * @throws IOException if server cannot bind to port
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        
        // Register endpoint handlers
        server.createContext("/api/files/upload", new UploadHandler());
        server.createContext("/api/files/", new DownloadHandler());
        server.createContext("/api/health", new HealthHandler());
        
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        
        System.out.println("LoadBalancerAPIServer listening on port " + port);
    }

    /**
     * Handler for file upload requests.
     * 
     * Receives HTTP POST from GUI client with:
     * - Body: raw file data
     * - Headers: X-File-Name, X-File-Size
     */
    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            String fileId = UUID.randomUUID().toString();
            String fileName = exchange.getRequestHeaders().getFirst("X-File-Name");
            String fileSizeStr = exchange.getRequestHeaders().getFirst("X-File-Size");
            
            long fileSize = 0;
            if (fileSizeStr != null) {
                try {
                    fileSize = Long.parseLong(fileSizeStr);
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "Invalid X-File-Size header");
                    return;
                }
            }

            // === Create request object and enqueue ===
            // This request will be processed by LoadBalancerWorker
            Request request = new Request(
                fileId,
                Request.Type.UPLOAD,
                fileSize,
                0  // base priority 0 (can be adjusted by client)
            );
            
            requestQueue.add(request);
            requestQueue.notifyNewRequest();

            System.out.println("[API] Upload request queued: " + fileId + 
                             " (" + fileName + ", " + fileSize + " bytes)");

            // === Return response to client ===
            // Client receives fileId and queue position
            String response = "{\"fileId\":\"" + fileId + 
                            "\",\"status\":\"queued\",\"fileName\":\"" + fileName + "\"}";
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }

    /**
     * Handler for file download requests.
     * 
     * Receives HTTP GET from GUI client for:
     * - URL: /api/files/{fileId}/download
     */
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            // Extract fileId from path /api/files/{fileId}/download
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            
            if (parts.length < 4) {
                sendError(exchange, 400, "Invalid request path");
                return;
            }
            
            String fileId = parts[3];

            // === Create download request and enqueue ===
            Request request = new Request(
                fileId,
                Request.Type.DOWNLOAD,
                0,  // size unknown for download
                1   // higher priority for downloads
            );
            
            requestQueue.add(request);
            requestQueue.notifyNewRequest();

            System.out.println("[API] Download request queued: " + fileId);

            // === Return queued response ===
            String response = "{\"fileId\":\"" + fileId + 
                            "\",\"status\":\"queued\"}";
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }

    /**
     * Handler for health check requests.
     * Used by GUI and monitoring systems to verify LB is operational.
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"healthy\",\"queue_size\":" + 
                            requestQueue.size() + "}";
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }

    /**
     * Send error response to client.
     */
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        exchange.sendResponseHeaders(code, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }
}