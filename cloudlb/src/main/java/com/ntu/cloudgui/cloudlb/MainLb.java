package com.ntu.cloudgui.cloudlb;

import com.ntu.cloudgui.cloudlb.cluster.HealthChecker;
import com.ntu.cloudgui.cloudlb.cluster.NodeRegistry;
import com.ntu.cloudgui.cloudlb.cluster.StorageNode;
import com.ntu.cloudgui.cloudlb.core.LoadBalancerWorker;
import com.ntu.cloudgui.cloudlb.core.Request;
import com.ntu.cloudgui.cloudlb.core.RequestQueue;
import com.ntu.cloudgui.cloudlb.core.RoundRobinScheduler;
import com.ntu.cloudgui.cloudlb.core.Scheduler;
import com.ntu.cloudgui.cloudlb.scaling.ScalingService;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainLb {

    private static final int HTTP_PORT = 8080;

    private static final String MQTT_BROKER_URL =
            System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://mqtt-broker:1883");
    private static final String MQTT_TOPIC =
            System.getenv().getOrDefault("MQTT_SCALE_TOPIC", "lb/scale/request");

    public static void main(String[] args) throws Exception {
        RequestQueue requestQueue = new RequestQueue();
        NodeRegistry registry = new NodeRegistry();
        Scheduler scheduler = new RoundRobinScheduler(); // or FcfsScheduler / SjnScheduler

        // Register sample storage nodes (adjust hostnames to your Docker compose)
        registry.addNode(new StorageNode("node-1", "aggservice-1", 8080));
        registry.addNode(new StorageNode("node-2", "aggservice-2", 8080));

        ScheduledExecutorService exec = Executors.newScheduledThreadPool(4);

        // Health checks
        HealthChecker healthChecker = new HealthChecker(registry, exec);
        healthChecker.start(5); // seconds

        // Worker that pulls from queue and forwards to healthy node
        LoadBalancerWorker worker = new LoadBalancerWorker(requestQueue, registry, scheduler);
        exec.submit(worker);

        // Scaling over MQTT (optional)
        try {
            String clientId = "lb-client-" + UUID.randomUUID();
            ScalingService scaling = new ScalingService(
                    requestQueue,
                    MQTT_BROKER_URL,
                    clientId,
                    MQTT_TOPIC
            );
            exec.scheduleAtFixedRate(
                    scaling::checkAndScale,
                    10,
                    10,
                    TimeUnit.SECONDS
            );
        } catch (MqttException e) {
            e.printStackTrace();
            System.err.println("ScalingService disabled (MQTT error).");
        }

        // Start HTTP API
        startHttpApi(requestQueue);

        System.out.println("Load Balancer started on port " + HTTP_PORT);
    }

    private static void startHttpApi(RequestQueue queue) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        HttpContext uploadCtx = server.createContext("/api/files/upload");
        uploadCtx.setHandler(exchange -> handleUpload(exchange, queue));

        HttpContext downloadCtx = server.createContext("/api/files");
        downloadCtx.setHandler(exchange -> handleDownload(exchange, queue));

        HttpContext healthCtx = server.createContext("/api/health");
        healthCtx.setHandler(MainLb::handleHealth);

        server.start();
    }

    // POST /api/files/upload
    private static void handleUpload(HttpExchange exchange, RequestQueue queue) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String fileName = exchange.getRequestHeaders().getFirst("X-File-Name");
        String fileId = exchange.getRequestHeaders().getFirst("X-File-ID");
        String sizeHeader = exchange.getRequestHeaders().getFirst("X-File-Size");

        if (fileName == null || fileId == null || sizeHeader == null) {
            sendText(exchange, 400, "Missing file headers");
            return;
        }

        long sizeBytes;
        try {
            sizeBytes = Long.parseLong(sizeHeader);
        } catch (NumberFormatException e) {
            sendText(exchange, 400, "Invalid X-File-Size");
            return;
        }

        // Drain request body (content is handed off via internal forwarding in worker)
        try (InputStream in = exchange.getRequestBody()) {
            byte[] buffer = new byte[8192];
            while (in.read(buffer) != -1) {
                // discard
            }
        }

        Request req = new Request(fileId, Request.Type.UPLOAD, sizeBytes, 0);
        queue.add(req);              // <- matches your RequestQueue API
        queue.notifyNewRequest();

        String json = "{\"fileId\":\"" + fileId + "\",\"status\":\"queued\"}";
        sendJson(exchange, 201, json);
    }

    // GET /api/files/{fileId}/download
    private static void handleDownload(HttpExchange exchange, RequestQueue queue) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath(); // /api/files/{fileId}/download
        String[] parts = path.split("/");
        if (parts.length < 5 || !"download".equals(parts[4])) {
            sendText(exchange, 400, "Invalid download path");
            return;
        }

        String fileId = parts[3];
        if (fileId == null || fileId.isBlank()) {
            sendText(exchange, 400, "Missing fileId");
            return;
        }

        Request req = new Request(fileId, Request.Type.DOWNLOAD, 0L, 0);
        queue.add(req);              // <- matches your RequestQueue API
        queue.notifyNewRequest();

        sendText(exchange, 200, "Download request queued for fileId=" + fileId);
    }

    // GET /api/health
    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        sendText(exchange, 200, "OK");
    }

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
