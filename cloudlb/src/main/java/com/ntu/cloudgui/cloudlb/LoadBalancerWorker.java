package com.ntu.cloudgui.cloudlb;

import com.ntu.cloudgui.cloudlb.core.NodeRegistry;
import com.ntu.cloudgui.cloudlb.core.Scheduler;
import com.ntu.cloudgui.cloudlb.core.StorageNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * LoadBalancerWorker Class - Request Processing Thread
 * 
 * Continuously processes requests from the RequestQueue by:
 * - Consuming requests in priority order (SJN + aging)
 * - Filtering out unhealthy storage nodes
 * - Selecting best node via Scheduler
 * - Forwarding requests to selected Aggregator node
 * - Implementing artificial latency simulation
 * - Comprehensive error handling and logging
 * 
 * Thread Safety: Designed to run as a single dedicated thread
 * Uses RequestQueue for thread-safe communication with API Server
 * 
 * Architecture:
 * - Consumer in producer-consumer pattern
 * - Reads from RequestQueue (blocking)
 * - Writes to Aggregator services (HTTP calls)
 * 
 * Usage:
 * ```
 * Thread workerThread = new Thread(new LoadBalancerWorker(
 *     requestQueue,
 *     nodeRegistry,
 *     scheduler
 * ));
 * workerThread.start();
 * ```
 */
public class LoadBalancerWorker implements Runnable {

    private static final long LATENCY_MIN_MS = 1000;  // 1 second
    private static final long LATENCY_MAX_MS = 5000;  // 5 seconds
    private static final int HTTP_TIMEOUT_MS = 30000; // 30 seconds
    private static final int LOG_TIMEOUT_MS = 5000;

    private final RequestQueue requestQueue;
    private final NodeRegistry nodeRegistry;
    private final Scheduler scheduler;
    private final Map<String, Semaphore> nodeLocks = new ConcurrentHashMap<>();
    private final long latencyMinMs;
    private final long latencyMaxMs;
    private final String logHost;
    private final int logPort;

    /**
     * Create a new LoadBalancerWorker.
     * 
     * @param requestQueue Queue to consume requests from
     * @param nodeRegistry Registry of available storage nodes
     * @param scheduler Scheduler for node selection (FCFS/SJN/RoundRobin)
     */
    public LoadBalancerWorker(RequestQueue requestQueue, 
                               NodeRegistry nodeRegistry, 
                               Scheduler scheduler) {
        this.requestQueue = requestQueue;
        this.nodeRegistry = nodeRegistry;
        this.scheduler = scheduler;
        this.latencyMinMs = readLongEnv("LB_DELAY_MS_MIN", LATENCY_MIN_MS);
        this.latencyMaxMs = Math.max(latencyMinMs, readLongEnv("LB_DELAY_MS_MAX", LATENCY_MAX_MS));
        this.logHost = System.getenv().getOrDefault("AGGREGATOR_HOST", "aggregator");
        this.logPort = readIntEnv("LB_LOG_PORT", 9100);
    }

    /**
     * Main worker thread loop.
     * 
     * Continuously:
     * 1. Consumes requests from queue (blocking)
     * 2. Filters healthy nodes
     * 3. Selects node via scheduler
     * 4. Forwards request to aggregator
     * 5. Implements latency simulation
     * 6. Logs results and errors
     * 
     * Runs indefinitely until thread is interrupted.
     */
    @Override
    public void run() {
        System.out.println("[LB Worker] Started - Ready to process requests");

        while (true) {
            try {
                System.out.println("[LB Worker] Waiting for requests...");

                // Blocking: Wait for next request from queue
                Request request = requestQueue.get();

                System.out.printf("[LB Worker] Received %s request: %s (%.2f MB)%n",
                    request.getType().getDisplayName(),
                    request.getId(),
                    request.getSizeMB());

                // Get all nodes from registry
                List<StorageNode> allNodes = nodeRegistry.getAllNodes();
                
                if (allNodes.isEmpty()) {
                    System.err.println("[LB Worker] ERROR: No storage nodes available!");
                    continue;
                }

                // Filter healthy nodes only
                List<StorageNode> healthyNodes = nodeRegistry.getHealthyNodes();
                
                if (healthyNodes.isEmpty()) {
                    System.err.printf("[LB Worker] ERROR: No healthy nodes (total: %d, healthy: 0)%n",
                        allNodes.size());
                    continue;
                }

                // Select node using scheduler
                StorageNode selectedNode = scheduler.selectNode(healthyNodes, request);
                
                if (selectedNode == null) {
                    System.err.println("[LB Worker] ERROR: Scheduler returned null node!");
                    continue;
                }

                System.out.printf("[LB Worker] Selected node: %s (%s)%n",
                    selectedNode.getName(),
                    selectedNode.getAddress());

                // Acquire lock for the selected node
                Semaphore lock = nodeLocks.computeIfAbsent(selectedNode.getName(), k -> new Semaphore(1));
                lock.acquire();

                try {
                    // Forward request to aggregator
                    boolean success = forwardToNode(selectedNode, request);

                    if (success) {
                        System.out.printf("[LB Worker] ✓ Successfully processed %s request%n",
                            request.getType().getDisplayName());
                    } else {
                        System.out.printf("[LB Worker] ✗ Failed to process %s request%n",
                            request.getType().getDisplayName());
                    }
                } finally {
                    lock.release();
                }

            } catch (InterruptedException e) {
                System.out.println("[LB Worker] Interrupted - Shutting down");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.printf("[LB Worker] Unexpected error: %s%n", e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[LB Worker] Stopped");
    }

    /**
     * Forward request to selected storage node.
     * 
     * Process:
     * 1. Determine request type (UPLOAD or DOWNLOAD)
     * 2. Build request body/URL
     * 3. Make HTTP call to aggregator
     * 4. Implement artificial latency (1-5 seconds)
     * 5. Handle errors gracefully
     * 
     * @param node Target storage node
     * @param request Request to forward
     * @return true if successful, false otherwise
     */
    private boolean forwardToNode(StorageNode node, Request request) {
        try {
            // Implement artificial latency (1-5 seconds)
            long latencyMs = latencyMinMs +
                            (long)(Math.random() * (latencyMaxMs - latencyMinMs));
            
            System.out.printf("[LB Worker] Simulating latency: %d ms%n", latencyMs);
            logTaskScheduled(request, node, latencyMs);
            Thread.sleep(latencyMs);

            // Forward based on request type
            if (request.isUpload()) {
                return forwardUpload(node, request);
            } else if (request.isDownload()) {
                return forwardDownload(node, request);
            } else {
                System.err.println("[LB Worker] ERROR: Unknown request type");
                return false;
            }

        } catch (InterruptedException e) {
            System.err.println("[LB Worker] Latency simulation interrupted");
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.err.printf("[LB Worker] Error forwarding request: %s%n", e.getMessage());
            return false;
        }
    }

    /**
     * Forward UPLOAD request to aggregator.
     * 
     * Makes HTTP POST request to:
     * http://node:8080/api/files/upload?fileId={id}&size={size}
     * 
     * @param node Target storage node
     * @param request Upload request
     * @return true if successful
     */
    private boolean forwardUpload(StorageNode node, Request request) {
        try {
            // Build upload URL
            String url = String.format("http://%s/api/files/upload?fileId=%s&size=%d",
                node.getAddress(),
                request.getId(),
                request.getSizeBytes());

            System.out.printf("[LB Worker] Uploading to: %s%n", url);

            FileBufferStore.FilePayload payload = FileBufferStore.take(request.getId());
            if (payload == null || payload.getContent() == null) {
                System.err.printf("[LB Worker] Upload error: no payload for %s%n", request.getId());
                return false;
            }

            // Make HTTP POST request
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("X-File-ID", request.getId());
            if (payload.getFileName() != null && !payload.getFileName().isBlank()) {
                connection.setRequestProperty("X-File-Name", payload.getFileName());
            }
            connection.setRequestProperty("X-File-Size", String.valueOf(payload.getContent().length));

            try {
                // Send file content
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getContent());
                    os.flush();
                }

                // Check response
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK || 
                    responseCode == HttpURLConnection.HTTP_CREATED) {
                    System.out.printf("[LB Worker] Upload successful (HTTP %d)%n", responseCode);
                    return true;
                } else {
                    System.err.printf("[LB Worker] Upload failed (HTTP %d)%n", responseCode);
                    return false;
                }
            } finally {
                connection.disconnect();
            }

        } catch (Exception e) {
            System.err.printf("[LB Worker] Upload error: %s%n", e.getMessage());
            return false;
        }
    }

    /**
     * Forward DOWNLOAD request to aggregator.
     * 
     * Makes HTTP GET request to:
     * http://node:8080/api/files/{id}/download
     * 
     * @param node Target storage node
     * @param request Download request
     * @return true if successful
     */
    private boolean forwardDownload(StorageNode node, Request request) {
        try {
            // Build download URL
            String url = String.format("http://%s/api/files/%s/download",
                node.getAddress(),
                request.getId());

            System.out.printf("[LB Worker] Downloading from: %s%n", url);

            // Make HTTP GET request
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);

            try {
                // Check response
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response body
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        
                        StringBuilder content = new StringBuilder();
                        String line;
                        int bytesRead = 0;
                        
                        while ((line = reader.readLine()) != null) {
                            content.append(line);
                            bytesRead += line.length();
                        }
                        
                        System.out.printf("[LB Worker] Download successful (HTTP %d, %d bytes)%n",
                            responseCode,
                            bytesRead);
                        return true;
                    }
                } else {
                    System.err.printf("[LB Worker] Download failed (HTTP %d)%n", responseCode);
                    return false;
                }
            } finally {
                connection.disconnect();
            }

        } catch (Exception e) {
            System.err.printf("[LB Worker] Download error: %s%n", e.getMessage());
            return false;
        }
    }

    /**
     * Get string representation of worker.
     * 
     * @return Worker status string
     */
    @Override
    public String toString() {
        return String.format("LoadBalancerWorker{queue_size=%d, healthy_nodes=%d}",
            requestQueue.size(),
            nodeRegistry.getHealthyNodes().size());
    }

    private void logTaskScheduled(Request request, StorageNode node, long latencyMs) {
        String description = String.format(
            "Task Scheduled: id=%s type=%s sizeBytes=%d scheduler=%s delayMs=%d node=%s timestamp=%s",
            request.getId(),
            request.getType().getDisplayName(),
            request.getSizeBytes(),
            scheduler.getName(),
            latencyMs,
            node.getAddress(),
            Instant.now().toString()
        );
        postSystemLog("TASK_SCHEDULED", description);
    }

    private void postSystemLog(String eventType, String description) {
        try {
            String payload = "event_type=" + URLEncoder.encode(eventType, StandardCharsets.UTF_8)
                + "&description=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                + "&severity=INFO"
                + "&service_name=load-balancer";
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);

            URL url = new URL(String.format("http://%s:%d/api/system-logs", logHost, logPort));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(LOG_TIMEOUT_MS);
            connection.setReadTimeout(LOG_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                System.err.printf("[LB Worker] Log ingestion failed (HTTP %d)%n", code);
            }
            connection.disconnect();
        } catch (Exception e) {
            System.err.printf("[LB Worker] Log ingestion error: %s%n", e.getMessage());
        }
    }

    private long readLongEnv(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int readIntEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
