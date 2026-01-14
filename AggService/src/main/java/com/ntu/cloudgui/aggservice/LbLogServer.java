package com.ntu.cloudgui.aggservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class LbLogServer {

    private final HttpServer server;
    private final DatabaseManager databaseManager;

    public LbLogServer(DatabaseManager databaseManager, int port) throws IOException {
        this.databaseManager = databaseManager;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/api/system-logs", new LogHandler());
        this.server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private class LogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String query = exchange.getRequestURI().getRawQuery();
            Map<String, String> params = parseParams(query, body);

            String eventType = params.getOrDefault("event_type", "").trim();
            String description = params.getOrDefault("description", "").trim();
            if (eventType.isEmpty() || description.isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String severity = params.getOrDefault("severity", "INFO").trim();
            String serviceName = params.getOrDefault("service_name", "load-balancer").trim();
            Long userId = parseUserId(params.get("user_id"));

            try {
                insertSystemLog(eventType, description, severity, serviceName, userId);
                byte[] response = "{\"status\":\"OK\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (SQLException e) {
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        }
    }

    private Map<String, String> parseParams(String query, String body) {
        Map<String, String> params = new HashMap<>();
        parseInto(params, query);
        parseInto(params, body);
        return params;
    }

    private void parseInto(Map<String, String> params, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = urlDecode(pair.substring(0, idx));
            String value = urlDecode(pair.substring(idx + 1));
            params.put(key, value);
        }
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private Long parseUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void insertSystemLog(String eventType, String description, String severity,
                                 String serviceName, Long userId) throws SQLException {
        String sql = "INSERT INTO System_Logs (event_type, user_id, description, severity, service_name) " +
            "VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, eventType);
                if (userId == null) {
                    stmt.setNull(2, java.sql.Types.BIGINT);
                } else {
                    stmt.setLong(2, userId);
                }
                stmt.setString(3, description);
                stmt.setString(4, severity);
                stmt.setString(5, serviceName);
                stmt.executeUpdate();
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }
}
