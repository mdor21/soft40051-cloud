package com.ntu.cloudgui.aggservice.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkStorageService {

    private static final String CHUNK_ROOT = "/data/chunks";
    private static final int MAX_RETRIES = 3;

    private final List<String> servers;
    private final AtomicInteger roundRobinCounter;
    private final String sftpUser;
    private final String sftpPassword;

    public ChunkStorageService(List<String> servers, String sftpUser, String sftpPassword) {
        this.servers = servers;
        this.sftpUser = sftpUser;
        this.sftpPassword = sftpPassword;
        this.roundRobinCounter = new AtomicInteger(0);
        System.out.println("ChunkStorageService initialized with " + servers.size() + " servers");
    }

    public String selectServer() throws ProcessingException {
        if (servers.isEmpty()) {
            throw new ProcessingException("No storage servers available", ErrorType.STORAGE_ERROR);
        }
        int index = Math.abs(roundRobinCounter.getAndIncrement()) % servers.size();
        return servers.get(index);
    }

    public String storeChunk(String serverHost, String fileId, int chunkIndex, byte[] encryptedData) throws ProcessingException {
        String remotePath = buildChunkPath(fileId, chunkIndex);
        int attempt = 0;
        ProcessingException lastException = null;
        while (attempt < MAX_RETRIES) {
            try {
                uploadChunk(serverHost, remotePath, encryptedData);
                return remotePath;
            } catch (ProcessingException e) {
                lastException = e;
                attempt++;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(Math.min(1000L * attempt, 5000L));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProcessingException("Upload interrupted", ErrorType.STORAGE_ERROR, ie);
                    }
                }
            }
        }
        throw lastException != null ? lastException : new ProcessingException("Failed to store chunk", ErrorType.STORAGE_ERROR);
    }

    public byte[] retrieveChunk(String serverHost, String remotePath) throws ProcessingException {
        int attempt = 0;
        ProcessingException lastException = null;
        while (attempt < MAX_RETRIES) {
            try {
                return downloadChunk(serverHost, remotePath);
            } catch (ProcessingException e) {
                lastException = e;
                attempt++;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(Math.min(1000L * attempt, 5000L));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProcessingException("Download interrupted", ErrorType.STORAGE_ERROR, ie);
                    }
                }
            }
        }
        throw lastException != null ? lastException : new ProcessingException("Failed to retrieve chunk", ErrorType.STORAGE_ERROR);
    }

    public void deleteChunk(String serverHost, String remotePath) throws ProcessingException {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sftpUser, serverHost, 22);
            session.setPassword(sftpPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.rm(remotePath);
            channelSftp.exit();
            session.disconnect();
        } catch (Exception e) {
            throw new ProcessingException("Failed to delete chunk: " + e.getMessage(), ErrorType.STORAGE_ERROR, e);
        }
    }

    private void uploadChunk(String serverHost, String remotePath, byte[] data) throws ProcessingException {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sftpUser, serverHost, 22);
            session.setPassword(sftpPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.put(new ByteArrayInputStream(data), remotePath);
            channelSftp.exit();
            session.disconnect();
        } catch (Exception e) {
            throw new ProcessingException("SFTP upload failed: " + e.getMessage(), ErrorType.STORAGE_ERROR, e);
        }
    }

    private byte[] downloadChunk(String serverHost, String remotePath) throws ProcessingException {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sftpUser, serverHost, 22);
            session.setPassword(sftpPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remotePath, outputStream);
            channelSftp.exit();
            session.disconnect();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new ProcessingException("SFTP download failed: " + e.getMessage(), ErrorType.STORAGE_ERROR, e);
        }
    }

    private String buildChunkPath(String fileId, int chunkIndex) {
        return String.format("%s/%s/chunk-%d", CHUNK_ROOT, fileId, chunkIndex);
    }
}
