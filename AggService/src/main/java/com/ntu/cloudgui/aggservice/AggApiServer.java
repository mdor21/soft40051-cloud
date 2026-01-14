package com.ntu.cloudgui.aggservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class AggApiServer {

    private final HttpServer httpServer;
    private final FileProcessingService fileProcessingService;
    private final FileMetadataRepository fileMetadataRepository;

    public AggApiServer(int port,
                        ExecutorService executor,
                        FileProcessingService fileProcessingService,
                        FileMetadataRepository fileMetadataRepository) throws IOException {
        this.fileProcessingService = fileProcessingService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer.createContext("/api/files/upload", new UploadHandler());
        this.httpServer.createContext("/api/files", new FileHandler());
        this.httpServer.setExecutor(executor);
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }

    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> params = parseParams(exchange.getRequestURI());
            String fileName = firstNonBlank(
                exchange.getRequestHeaders().getFirst("X-File-Name"),
                params.get("fileName"),
                params.get("filename")
            );
            String fileId = firstNonBlank(
                exchange.getRequestHeaders().getFirst("X-File-ID"),
                params.get("fileId")
            );
            String username = firstNonBlank(
                exchange.getRequestHeaders().getFirst("X-Username"),
                params.get("username")
            );

            byte[] fileData = exchange.getRequestBody().readAllBytes();
            if (fileName == null || fileName.isBlank()) {
                fileName = (fileId == null || fileId.isBlank()) ? "upload.bin" : fileId;
            }

            try {
                String storedFileId = fileProcessingService.processAndStoreFile(fileName, fileData, username, fileId);
                String response = "{\"fileId\":\"" + escapeJson(storedFileId) + "\",\"status\":\"uploaded\"}";
                sendJson(exchange, 200, response);
            } catch (ProcessingException e) {
                sendJson(exchange, 500, "{\"error\":\"Upload failed\"}");
            }
        }
    }

    private class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            Map<String, String> params = parseParams(uri);

            String[] parts = path.split("/");
            String fileId = null;
            String action = null;
            if (parts.length >= 5) {
                // /api/files/{fileId}/{action}
                fileId = parts[3];
                action = parts[4];
            } else {
                fileId = params.get("fileId");
                action = params.get("action");
            }

            if (fileId == null || fileId.isBlank() || action == null || action.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Invalid request format\"}");
                return;
            }

            String username = firstNonBlank(
                exchange.getRequestHeaders().getFirst("X-Username"),
                params.get("username")
            );

            if ("download".equalsIgnoreCase(action)) {
                handleDownload(exchange, fileId, username);
            } else if ("delete".equalsIgnoreCase(action)) {
                handleDelete(exchange, fileId, username);
            } else {
                sendJson(exchange, 404, "{\"error\":\"Unsupported action\"}");
            }
        }

        private void handleDownload(HttpExchange exchange, String fileId, String username) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                byte[] fileData = fileProcessingService.retrieveAndReassembleFile(fileId, username);
                FileMetadata metadata = fileMetadataRepository.findByFileId(fileId);
                if (metadata != null && metadata.getOriginalFilename() != null) {
                    exchange.getResponseHeaders().set("X-File-Name", metadata.getOriginalFilename());
                }
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, fileData.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(fileData);
                }
            } catch (ProcessingException e) {
                sendJson(exchange, 404, "{\"error\":\"File not found\"}");
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"Download failed\"}");
            }
        }

        private void handleDelete(HttpExchange exchange, String fileId, String username) throws IOException {
            if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())
                && !"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                fileProcessingService.deleteFile(fileId, username);
                sendJson(exchange, 200, "{\"status\":\"deleted\",\"fileId\":\"" + escapeJson(fileId) + "\"}");
            } catch (ProcessingException e) {
                sendJson(exchange, 500, "{\"error\":\"Delete failed\"}");
            }
        }
    }

    private Map<String, String> parseParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = urlDecode(pair.substring(0, idx));
            String value = urlDecode(pair.substring(idx + 1));
            params.put(key, value);
        }
        return params;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
        }
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
