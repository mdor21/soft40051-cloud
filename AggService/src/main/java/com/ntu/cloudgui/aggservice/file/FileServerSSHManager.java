package com.ntu.cloudgui.aggservice.file;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * File Server SSH Manager (JSch)
 * 
 * Handles encrypted SSH connections to file server containers.
 * 
 * Connection Details:
 * - Protocol: SSH (OpenSSH)
 * - Port: 22 (inside file server containers)
 * - Authentication: Username/Password or Key-based
 * - Encryption: All file chunks transmitted over SSH tunnel
 * 
 * File Servers:
 * - soft40051-files-container1 (file-server-1:22)
 * - soft40051-files-container2 (file-server-2:22)
 * - soft40051-files-container3 (file-server-3:22)
 * - soft40051-files-container4 (file-server-4:22)
 */
@Slf4j
@Service
public class FileServerSSHManager {
    
    private static final int SSH_PORT = 22;
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final String SSH_USER = "fileserver";
    private static final String SSH_PASSWORD = "secure123"; // TODO: Use env vars / secrets
    
    private final JSch jsch;
    private final Map<String, Session> activeSessions = Collections.synchronizedMap(new HashMap<>());
    
    // File server hostnames in Docker network
    private static final String[] FILE_SERVERS = {
        "file-server-1",
        "file-server-2",
        "file-server-3",
        "file-server-4"
    };
    
    /**
     * Constructor - Initialize JSch
     */
    public FileServerSSHManager() {
        this.jsch = new JSch();
        log.info("FileServerSSHManager initialized with JSch");
    }
    
    /**
     * Upload chunk to file server via SSH
     * 
     * @param fileServerIndex Target file server (0-3)
     * @param chunkId Unique chunk identifier
     * @param chunkData Chunk data to upload
     * @param checksum CRC32 checksum for verification
     * @throws JSchException on SSH connection error
     */
    public void uploadChunk(int fileServerIndex, String chunkId, byte[] chunkData, String checksum) 
            throws JSchException {
        
        if (fileServerIndex < 0 || fileServerIndex >= FILE_SERVERS.length) {
            throw new IllegalArgumentException("Invalid file server index: " + fileServerIndex);
        }
        
        String hostname = FILE_SERVERS[fileServerIndex];
        Session session = getOrCreateSession(hostname);
        
        try {
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(CONNECTION_TIMEOUT_MS);
            
            // Create remote file with metadata
            String remoteFilename = String.format("/data/chunks/%s.chunk", chunkId);
            sftp.put(new ByteArrayInputStream(chunkData), remoteFilename);
            
            // Store checksum in metadata file
            String metadataFilename = String.format("/data/chunks/%s.metadata", chunkId);
            String metadata = String.format("checksum=%s\nsize=%d\n", checksum, chunkData.length);
            sftp.put(new ByteArrayInputStream(metadata.getBytes()), metadataFilename);
            
            sftp.disconnect();
            
            log.info("Uploaded chunk {} to {} ({} bytes, CRC32: {})",
                chunkId, hostname, chunkData.length, checksum);
                
        } catch (SftpException e) {
            throw new JSchException("SFTP upload failed to " + hostname, e);
        }
    }
    
    /**
     * Download chunk from file server via SSH
     * 
     * @param fileServerIndex Source file server (0-3)
     * @param chunkId Chunk identifier
     * @return Chunk data
     * @throws JSchException on SSH connection error
     */
    public byte[] downloadChunk(int fileServerIndex, String chunkId) throws JSchException {
        
        if (fileServerIndex < 0 || fileServerIndex >= FILE_SERVERS.length) {
            throw new IllegalArgumentException("Invalid file server index: " + fileServerIndex);
        }
        
        String hostname = FILE_SERVERS[fileServerIndex];
        Session session = getOrCreateSession(hostname);
        
        try {
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(CONNECTION_TIMEOUT_MS);
            
            String remoteFilename = String.format("/data/chunks/%s.chunk", chunkId);
            InputStream inputStream = sftp.get(remoteFilename);
            byte[] data = inputStream.readAllBytes();
            inputStream.close();
            
            sftp.disconnect();
            
            log.info("Downloaded chunk {} from {} ({} bytes)",
                chunkId, hostname, data.length);
            
            return data;
            
        } catch (Exception e) {
            throw new JSchException("SFTP download failed from " + hostname, e);
        }
    }
    
    /**
     * Get or create SSH session to file server
     * 
     * @param hostname File server hostname
     * @return Active SSH session
     * @throws JSchException on connection error
     */
    private Session getOrCreateSession(String hostname) throws JSchException {
        if (activeSessions.containsKey(hostname)) {
            Session session = activeSessions.get(hostname);
            if (session.isConnected()) {
                return session;
            }
        }
        
        // Create new session
        Session session = jsch.getSession(SSH_USER, hostname, SSH_PORT);
        session.setPassword(SSH_PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "password");
        session.connect(CONNECTION_TIMEOUT_MS);
        
        activeSessions.put(hostname, session);
        log.info("SSH connection established to {}", hostname);
        
        return session;
    }
    
    /**
     * Close all SSH sessions
     */
    public void closeAllSessions() {
        for (Session session : activeSessions.values()) {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        activeSessions.clear();
        log.info("All SSH sessions closed");
    }
    
    /**
     * Helper class for byte array streams
     */
    private static class ByteArrayInputStream extends java.io.ByteArrayInputStream {
        ByteArrayInputStream(byte[] buf) {
            super(buf);
        }
    }
}
