package com.ntu.cloudgui.cloudlb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * LoadBalancer Client Library
 * 
 * Provides clean API for GUI and other clients to communicate with the Load Balancer.
 * Handles file upload/download operations and health checks.
 * 
 * Usage:
 * ```
 * LoadBalancerClient client = new LoadBalancerClient("http://lb:8080");
 * String fileId = client.uploadFile("document.pdf", fileBytes);
 * byte[] content = client.downloadFile(fileId);
 * HealthStatus status = client.getHealth();
 * ```
 * 
 * Thread Safety: Thread-safe (HttpClient is thread-safe)
 */
public class LoadBalancerClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    
    private final String baseUrl;
    private final HttpClient httpClient;

    /**
     * Create a new LoadBalancer client.
     * 
     * @param baseUrl Base URL of the load balancer
     *                Example: "http://localhost:8080" or "http://lb:8080"
     */
    public LoadBalancerClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Upload a file to the load balancer.
     * 
     * The file is queued for processing and distributed to a storage node.
     * 
     * @param fileName Name of the file being uploaded
     * @param fileContent File content as byte array
     * @return fileId assigned by the load balancer for tracking
     * @throws Exception if upload fails (network, server error, etc.)
     */
    public String uploadFile(String fileName, byte[] fileContent) throws Exception {
        System.out.println("[Client] Uploading: " + fileName + 
                         " (" + (fileContent.length / 1_000_000.0) + " MB)");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(baseUrl + "/api/files/upload"))
            .header("X-File-Name", fileName)
            .header("X-File-Size", String.valueOf(fileContent.length))
            .POST(HttpRequest.BodyPublishers.ofByteArray(fileContent))
            .timeout(DEFAULT_TIMEOUT)
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse JSON response to extract fileId
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String fileId = json.get("fileId").getAsString();
            System.out.println("[Client] ✓ Upload successful. File ID: " + fileId);
            return fileId;
        } else {
            throw new Exception("Upload failed (HTTP " + response.statusCode() + "): " + 
                              response.body());
        }
    }

    /**
     * Download a file from the load balancer.
     * 
     * Retrieves the file content by its ID.
     * 
     * @param fileId ID of the file to download
     * @return File content as byte array
     * @throws Exception if download fails (not found, network error, etc.)
     */
    public byte[] downloadFile(String fileId) throws Exception {
        System.out.println("[Client] Downloading: " + fileId);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(baseUrl + "/api/files/" + fileId + "/download"))
            .GET()
            .timeout(DEFAULT_TIMEOUT)
            .build();

        HttpResponse<byte[]> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            byte[] fileContent = response.body();
            System.out.println("[Client] ✓ Download successful. Received " + 
                             (fileContent.length / 1_000_000.0) + " MB");
            return fileContent;
        } else {
            throw new Exception("Download failed (HTTP " + response.statusCode() + ")");
        }
    }

    /**
     * Check load balancer health status.
     * 
     * Returns current health status and queue information.
     * 
     * @return HealthStatus with status and queue size
     * @throws Exception if health check fails
     */
    public HealthStatus getHealth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(baseUrl + "/api/health"))
            .GET()
            .timeout(Duration.ofSeconds(5))
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String status = json.get("status").getAsString();
            int queueSize = json.get("queue_size").getAsInt();
            
            System.out.println("[Client] Health: " + status + " | Queue: " + queueSize);
            return new HealthStatus(status, queueSize);
        } else {
            throw new Exception("Health check failed (HTTP " + response.statusCode() + ")");
        }
    }

    /**
     * Health status response from LoadBalancer.
     * 
     * Contains current operational status and queue information.
     */
    public static class HealthStatus {
        public final String status;
        public final int queueSize;

        /**
         * Create a new HealthStatus.
         * 
         * @param status Current load balancer status
         * @param queueSize Number of pending requests in queue
         */
        public HealthStatus(String status, int queueSize) {
            this.status = status;
            this.queueSize = queueSize;
        }

        @Override
        public String toString() {
            return "HealthStatus{status='" + status + "', queueSize=" + queueSize + "}";
        }
    }
}
