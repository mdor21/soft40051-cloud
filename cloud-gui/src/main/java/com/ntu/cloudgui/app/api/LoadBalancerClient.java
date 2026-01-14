package com.ntu.cloudgui.app.api;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Load Balancer TCP/IP Client
 * 
 * Handles communication between JavaFX GUI and Load Balancer service.
 * - Protocol: TCP/IP over Docker network (load-balancer:6869)
 * - Message Format: JSON
 * 
 * Supported Operations:
 * - UPLOAD: Send file to aggregator via load balancer
 * - DOWNLOAD: Request file from aggregator via load balancer
 * - STATUS: Get system health and node status
 */
public class LoadBalancerClient {
    
    private static final String LB_HOST = System.getenv().getOrDefault("LB_HOST", "load-balancer");
    private static final int LB_PORT = Integer.parseInt(System.getenv().getOrDefault("LB_PORT", "6869"));
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 30000;
    
    private final Gson gson = new Gson();
    private volatile Socket socket;
    
    /**
     * Constructor - Initializes client (does not connect immediately)
     */
    public LoadBalancerClient() {
    }
    
    /**
     * Upload file via Load Balancer
     * 
     * @param filename Name of file
     * @param fileSize Size in bytes
     * @param fileData File contents
     * @return CompletableFuture with upload response
     */
    public CompletableFuture<UploadResponse> uploadFile(String filename, long fileSize, byte[] fileData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "UPLOAD");
                request.addProperty("filename", filename);
                request.addProperty("size", fileSize);
                request.addProperty("checksum", calculateCRC32(fileData));
                
                String response = sendRequestWithData(request.toString(), fileData);
                return gson.fromJson(response, UploadResponse.class);
                
            } catch (Exception e) {
                throw new RuntimeException("Upload failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Download file via Load Balancer
     * 
     * @param filename Name of file to download
     * @return CompletableFuture with download response
     */
    public CompletableFuture<DownloadResponse> downloadFile(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "DOWNLOAD");
                request.addProperty("filename", filename);
                
                String response = sendRequest(request.toString());
                return gson.fromJson(response, DownloadResponse.class);
                
            } catch (Exception e) {
                throw new RuntimeException("Download failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get Load Balancer system status
     * 
     * @return System status (node health, queue size, etc.)
     */
    public CompletableFuture<StatusResponse> getStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "STATUS");
                
                String response = sendRequest(request.toString());
                return gson.fromJson(response, StatusResponse.class);
                
            } catch (Exception e) {
                throw new RuntimeException("Status request failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Send request to Load Balancer and receive response
     * 
     * @param requestJson JSON request payload
     * @return Response string
     * @throws IOException on network error
     */
    private String sendRequest(String requestJson) throws IOException {
        try (Socket sock = new Socket(LB_HOST, LB_PORT)) {
            sock.setSoTimeout(SOCKET_TIMEOUT_MS);
            
            // Send request
            OutputStream out = sock.getOutputStream();
            out.write(requestJson.getBytes(StandardCharsets.UTF_8));
            out.write('\n');
            out.flush();
            
            // Receive response
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            return response.toString();
            
        } catch (IOException e) {
            throw new IOException("Failed to communicate with Load Balancer (" + LB_HOST + ":" + LB_PORT + ")", e);
        }
    }

    private String sendRequestWithData(String requestJson, byte[] data) throws IOException {
        try (Socket sock = new Socket(LB_HOST, LB_PORT)) {
            sock.setSoTimeout(SOCKET_TIMEOUT_MS);

            // Send request
            OutputStream out = sock.getOutputStream();
            out.write(requestJson.getBytes(StandardCharsets.UTF_8));
            out.write('\n');

            // Send binary data
            if (data != null) {
                out.write(data);
            }
            out.flush();
            sock.shutdownOutput(); // Signal end of sending

            // Receive response
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();

        } catch (IOException e) {
            throw new IOException("Failed to communicate with Load Balancer (" + LB_HOST + ":" + LB_PORT + ")", e);
        }
    }
    
    /**
     * Calculate CRC32 checksum for file data
     * 
     * @param data File data
     * @return CRC32 checksum as hex string
     */
    private String calculateCRC32(byte[] data) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(data);
        return String.format("%08X", crc.getValue());
    }
    
    /**
     * Response DTOs
     */
    public static class UploadResponse {
        public String status;
        public String message;
        public String fileId;
        public long timestamp;
    }
    
    public static class DownloadResponse {
        public String status;
        public String message;
        public byte[] fileData;
        public String checksum;
    }
    
    public static class StatusResponse {
        public String status;
        public int activeNodes;
        public int totalNodes;
        public int queueSize;
        public String schedulerType;
    }

    public static class FileMetadata {
        public String filename;
        public long size;
        public long createdAt;
        public long updatedAt;
    }

    public static class ListFilesResponse {
        public String status;
        public String message;
        public java.util.List<FileMetadata> files;
    }

    public static class DeleteResponse {
        public String status;
        public String message;
    }

    public static class ShareResponse {
        public String status;
        public String message;
    }

    public CompletableFuture<ListFilesResponse> listFiles(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "LIST_FILES");
                request.addProperty("username", username);
                String response = sendRequest(request.toString());
                return gson.fromJson(response, ListFilesResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("List files failed: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<UploadResponse> createFile(String filename, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "CREATE_FILE");
                request.addProperty("filename", filename);
                request.addProperty("username", username);
                String response = sendRequest(request.toString());
                return gson.fromJson(response, UploadResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Create file failed: " + e.getMessage(), e);
            }
        });
    }

    // TODO: This method, like uploadFile, does not actually transmit the file data.
    // The communication protocol needs to be updated to handle streaming binary data
    // after the initial JSON request.
    public CompletableFuture<UploadResponse> updateFile(String filename, long fileSize, byte[] fileData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "UPDATE_FILE");
                request.addProperty("filename", filename);
                request.addProperty("size", fileSize);
                request.addProperty("checksum", calculateCRC32(fileData));
                String response = sendRequestWithData(request.toString(), fileData);
                return gson.fromJson(response, UploadResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Update failed: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<DeleteResponse> deleteFile(String filename, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "DELETE_FILE");
                request.addProperty("filename", filename);
                request.addProperty("username", username);
                String response = sendRequest(request.toString());
                return gson.fromJson(response, DeleteResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Delete failed: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<ShareResponse> shareFile(String filename, String ownerUsername, String targetUsername, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "SHARE_FILE");
                request.addProperty("filename", filename);
                request.addProperty("owner_username", ownerUsername);
                request.addProperty("target_username", targetUsername);
                request.addProperty("permission", permission);
                String response = sendRequest(request.toString());
                return gson.fromJson(response, ShareResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Share failed: " + e.getMessage(), e);
            }
        });
    }
}
