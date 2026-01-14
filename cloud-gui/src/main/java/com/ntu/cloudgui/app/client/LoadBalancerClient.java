package com.ntu.cloudgui.app.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * HTTP client for communicating with Load Balancer microservice.
 *
 * Connectivity:
     * - Host: load-balancer (Docker service name)
     * - Port: 6869
 * - Base path: /api
 */
public class LoadBalancerClient {

    // Base URL for Load Balancer API (override via environment if needed)
    private static final String LB_BASE_URL = System.getenv()
            .getOrDefault("LB_BASE_URL", "http://load-balancer:6869/api");

    private static final int TIMEOUT_MS = 30_000;

    /**
     * Upload a file via the Load Balancer.
     *
     * @param fileStream file content
     * @param fileName   original filename
     * @param fileSize   file size in bytes
     * @return fileId assigned by backend
     * @throws IOException on network or protocol error
     */
    public static String uploadFile(InputStream fileStream,
                                    String fileName,
                                    long fileSize) throws IOException {
        return uploadFile(fileStream, fileName, fileSize, null);
    }

    public static String uploadFile(InputStream fileStream,
                                    String fileName,
                                    long fileSize,
                                    String fileId) throws IOException {
        if (fileId == null || fileId.isBlank()) {
            fileId = UUID.randomUUID().toString();
        }

        URL url = new URL(LB_BASE_URL + "/files/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("X-File-Name", fileName);
        conn.setRequestProperty("X-File-ID", fileId);
        conn.setRequestProperty("X-File-Size", String.valueOf(fileSize));

        // send body
        try (OutputStream out = conn.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK
                && code != HttpURLConnection.HTTP_CREATED) {
            throw new IOException("Upload failed, HTTP " + code);
        }

        // Expect a simple JSON line: {"fileId":"<uuid>","status":"uploaded"}
        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = reader.readLine();
            if (line != null && line.contains("fileId")) {
                // naive parsing; fine for simple responses
                String[] parts = line.split("\"");
                if (parts.length >= 4) {
                    fileId = parts[3];
                }
            }
        } finally {
            conn.disconnect();
        }

        return fileId;
    }

    /**
     * Download a file via the Load Balancer.
     *
     * @param fileId file identifier previously returned by uploadFile
     * @return InputStream with file data (caller must close)
     * @throws IOException on network or protocol error
     */
    public static InputStream downloadFile(String fileId) throws IOException {
        URL url = new URL(LB_BASE_URL + "/files/" + fileId + "/download");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("Download failed, HTTP " + code);
        }

        // Caller is responsible for closing the returned stream.
        return conn.getInputStream();
    }

    /**
     * Simple health check against the Load Balancer.
     *
     * @return true if HTTP 200 is returned, false otherwise
     */
    public static boolean isHealthy() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(LB_BASE_URL + "/health");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);

            int code = conn.getResponseCode();
            return code == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            System.err.println("Load Balancer health check failed: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private LoadBalancerClient() {
        // utility class; prevent instantiation
    }
}
