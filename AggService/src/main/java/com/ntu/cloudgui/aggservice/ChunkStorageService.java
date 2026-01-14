package com.ntu.cloudgui.aggservice;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class ChunkStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkStorageService.class);
    private final List<String> fileServerHosts;
    private final Map<String, Semaphore> fileServerSemaphores;
    private final String sftpUser;
    private final String sftpPass;
    private final int sftpPort;
    private int nextServer = 0;

    public ChunkStorageService(Configuration config) {
        this.fileServerHosts = config.getFileServerHosts();
        this.fileServerSemaphores = fileServerHosts.stream()
                .collect(Collectors.toConcurrentMap(host -> host, host -> new Semaphore(1)));
        this.sftpUser = System.getenv("SFTP_USER");
        this.sftpPass = System.getenv("SFTP_PASS");
        this.sftpPort = resolveSftpPort();
        logger.info("ChunkStorageService initialized for hosts: {}", String.join(", ", fileServerHosts));
    }

    public String storeChunk(byte[] chunkData, String fileId, int chunkIndex) throws ProcessingException {
        String server = selectNextServer();
        // storage_path is recorded under the /data storage root (fileId/chunk_N.enc).
        String remotePath = String.format("/data/%s/chunk_%d.enc", fileId, chunkIndex);
        Semaphore semaphore = fileServerSemaphores.get(server);

        if (semaphore == null) {
            throw new ProcessingException("No semaphore found for server: " + server, null);
        }

        try {
            semaphore.acquire();
            logger.debug("Acquired semaphore for server: {}", server);
            upload(server, sftpUser, sftpPass, sftpPort, new ByteArrayInputStream(chunkData), remotePath);
            return server;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Interrupted while waiting to upload chunk " + chunkIndex, e);
        } catch (JSchException | SftpException e) {
            throw new ProcessingException("Failed to upload chunk " + chunkIndex + " to " + server, e);
        } finally {
            semaphore.release();
            logger.debug("Released semaphore for server: {}", server);
        }
    }

    public byte[] retrieveChunk(String server, String fileId, int chunkIndex) throws ProcessingException {
        String remotePath = String.format("/data/%s/chunk_%d.enc", fileId, chunkIndex);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Semaphore semaphore = fileServerSemaphores.get(server);

        if (semaphore == null) {
            throw new ProcessingException("No semaphore found for server: " + server, null);
        }

        try {
            semaphore.acquire();
            logger.debug("Acquired semaphore for server: {}", server);
            download(server, sftpUser, sftpPass, sftpPort, remotePath, outputStream);
            return outputStream.toByteArray();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Interrupted while waiting to download chunk " + chunkIndex, e);
        } catch (JSchException | SftpException e) {
            throw new ProcessingException("Failed to download chunk " + chunkIndex + " from " + server, e);
        } finally {
            semaphore.release();
            logger.debug("Released semaphore for server: {}", server);
        }
    }

    public void deleteChunk(String server, String fileId, int chunkIndex) throws ProcessingException {
        String remotePath = String.format("/data/%s/chunk_%d.enc", fileId, chunkIndex);
        Semaphore semaphore = fileServerSemaphores.get(server);

        if (semaphore == null) {
            throw new ProcessingException("No semaphore found for server: " + server, null);
        }

        try {
            semaphore.acquire();
            logger.debug("Acquired semaphore for server: {}", server);
            delete(server, sftpUser, sftpPass, sftpPort, remotePath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Interrupted while waiting to delete chunk " + chunkIndex, e);
        } catch (JSchException | SftpException e) {
            throw new ProcessingException("Failed to delete chunk " + chunkIndex + " from " + server, e);
        } finally {
            semaphore.release();
            logger.debug("Released semaphore for server: {}", server);
        }
    }

    private synchronized String selectNextServer() {
        String server = fileServerHosts.get(nextServer);
        nextServer = (nextServer + 1) % fileServerHosts.size();
        return server;
    }

    private int resolveSftpPort() {
        String rawPort = System.getenv("SFTP_PORT");
        if (rawPort == null || rawPort.isBlank()) {
            return 22;
        }
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException e) {
            logger.warn("Invalid SFTP_PORT '{}', falling back to 22", rawPort);
            return 22;
        }
    }

    protected void upload(String host, String user, String password, int port, InputStream in, String remotePath) throws JSchException, SftpException {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // Ensure directory exists
            String remoteDir = remotePath.substring(0, remotePath.lastIndexOf('/'));
            try {
                channelSftp.cd(remoteDir);
            } catch (SftpException e) {
                // Directory does not exist, create it
                channelSftp.mkdir(remoteDir);
            }

            channelSftp.put(in, remotePath);
            logger.info("Successfully uploaded to {} at {}", host, remotePath);
        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }

    private void download(String host, String user, String password, int port, String remotePath, ByteArrayOutputStream out) throws JSchException, SftpException {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.get(remotePath, out);
            logger.info("Successfully downloaded from {} at {}", host, remotePath);
        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }

    private void delete(String host, String user, String password, int port, String remotePath) throws JSchException, SftpException {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.rm(remotePath);
            logger.info("Successfully deleted from {} at {}", host, remotePath);
        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
