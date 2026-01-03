package com.ntu.cloudgui.cloudlb;

import com.ntu.cloudgui.cloudlb.core.NodeRegistry;
import com.ntu.cloudgui.cloudlb.core.Scheduler;
import com.ntu.cloudgui.cloudlb.core.StorageNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    private final RequestQueue requestQueue;
    private final NodeRegistry nodeRegistry;
    private final Scheduler scheduler;

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

                // Forward request to aggregator
                boolean success = forwardToNode(selectedNode, request);

                if (success) {
                    System.out.printf("[LB Worker] ✓ Successfully processed %s request%n",
                        request.getType().getDisplayName());
                } else {
                    System.out.printf("[LB Worker] ✗ Failed to process %s request%n",
                        request.getType().getDisplayName());
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
            long latencyMs = LATENCY_MIN_MS + 
                            (long)(Math.random() * (LATENCY_MAX_MS - LATENCY_MIN_MS));
            
            System.out.printf("[LB Worker] Simulating latency: %d ms%n", latencyMs);
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

            // Make HTTP POST request
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setDoOutput(true);

            try {
                // Send dummy file content
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] dummyContent = new byte[(int) Math.min(request.getSizeBytes(), 1024)];
                    os.write(dummyContent);
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
}