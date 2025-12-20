package com.ntu.cloudgui.aggservice;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Main entry point for the Aggregator microservice.
 *
 * Responsibilities:
 * - Start an embedded HTTP server.
 * - Expose REST endpoints so the Load Balancer can call:
 *     POST /files/upload
 *     GET  /files/{fileId}/download
 *
 * The actual encryption, chunking, CRC, and MySQL work will be
 * implemented later in service classes (FileProcessingService, etc.).
 */
public class AggService {

    private static final int HTTP_PORT = 8080;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        // Upload endpoint
        HttpContext uploadCtx = server.createContext("/files/upload");
        uploadCtx.setHandler(AggService::handleUpload);

        // Download endpoint
        HttpContext downloadCtx = server.createContext("/files");
        downloadCtx.setHandler(AggService::handleDownload);

        server.start();
        System.out.println("AggService started on port " + HTTP_PORT);
    }

    /**
     * Handle POST /files/upload
     * For now this just consumes the body and returns a dummy fileId.
     * Later you will delegate to FileProcessingService.encryptAndChunk(...).
     */
    private static void handleUpload(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        // In a full implementation, read metadata from headers and pass to service.
        String originalFileName = exchange.getRequestHeaders().getFirst("X-File-Name");

        // Consume file stream (you will later stream into FileProcessingService)
        try (InputStream in = exchange.getRequestBody()) {
            byte[] buf = new byte[8192];
            while (in.read(buf) != -1) {
                // discard for now
            }
        }

        // TODO: call FileProcessingService.encryptAndChunk(...) and get real fileId
        String fileId = java.util.UUID.randomUUID().toString();

        String json = "{\"fileId\":\"" + fileId + "\",\"status\":\"uploaded\"}";
        sendJson(exchange, 201, json);
        System.out.println("AggService: uploaded file " + originalFileName + " as " + fileId);
    }

    /**
     * Handle GET /files/{fileId}/download
     * For now, returns a simple text payload; later this should stream the
     * reconstructed and decrypted file bytes.
     */
    private static void handleDownload(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath(); // /files/{fileId}/download
        String[] parts = path.split("/");
        if (parts.length < 4 || !"download".equals(parts[3])) {
            sendText(exchange, 400, "Invalid download path");
            return;
        }

        String fileId = parts[2];

        // TODO: call FileProcessingService.reconstructAndDecrypt(fileId, outputStream)
        String placeholder = "AggService would stream reconstructed file for id=" + fileId;
        byte[] data = placeholder.getBytes();

        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
        System.out.println("AggService: download requested for fileId=" + fileId);
    }

    // ---------- helper methods ----------

    private static void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
