package com.ntu.cloudgui.aggservice;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ChunkStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkStorageService.class);
    private final List<String> fileServerHosts;
    private final Semaphore fileServerSemaphore;
    private final String sftpUser;
    private final String sftpPass;
    private int nextServer = 0;

    public ChunkStorageService(Configuration config, Semaphore fileServerSemaphore) {
        this.fileServerHosts = config.getFileServerHosts();
        this.fileServerSemaphore = fileServerSemaphore;
        this.sftpUser = System.getenv("SFTP_USER");
        this.sftpPass = System.getenv("SFTP_PASS");
    }

    public String storeChunk(byte[] chunkData, long fileId, int chunkIndex) throws ProcessingException {
        String server = selectNextServer();
        String remotePath = String.format("/data/%d/chunk_%d.enc", fileId, chunkIndex);

        try {
            fileServerSemaphore.acquire();
            logger.debug("Acquired semaphore for file server access.");
            upload(server, sftpUser, sftpPass, 22, new ByteArrayInputStream(chunkData), remotePath);
            return server;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Interrupted while waiting to upload chunk " + chunkIndex, e);
        } catch (JSchException | SftpException e) {
            throw new ProcessingException("Failed to upload chunk " + chunkIndex + " to " + server, e);
        } finally {
            fileServerSemaphore.release();
            logger.debug("Released semaphore for file server access.");
        }
    }

    public byte[] retrieveChunk(String server, long fileId, int chunkIndex) throws ProcessingException {
        String remotePath = String.format("/data/%d/chunk_%d.enc", fileId, chunkIndex);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            fileServerSemaphore.acquire();
            logger.debug("Acquired semaphore for file server access.");
            download(server, sftpUser, sftpPass, 22, remotePath, outputStream);
            return outputStream.toByteArray();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Interrupted while waiting to download chunk " + chunkIndex, e);
        } catch (JSchException | SftpException e) {
            throw new ProcessingException("Failed to download chunk " + chunkIndex + " from " + server, e);
        } finally {
            fileServerSemaphore.release();
            logger.debug("Released semaphore for file server access.");
        }
    }

    public void deleteChunk(String server, long fileId, int chunkIndex) throws ProcessingException {
        String remotePath = String.format("/data/%d/chunk_%d.enc", fileId, chunkIndex);
        try {
            fileServerSemaphore.acquire();
            logger.debug("Acquired semaphore for file server access.");
            delete(server, sftpUser, sftpPass, 22, remotePath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("Interrupted while waiting to delete chunk " + chunkIndex, e);
        } catch (JSchException | SftpException e) {
            throw new ProcessingException("Failed to delete chunk " + chunkIndex + " from " + server, e);
        } finally {
            fileServerSemaphore.release();
            logger.debug("Released semaphore for file server access.");
        }
    }

    private String selectNextServer() {
        String server = fileServerHosts.get(nextServer);
        nextServer = (nextServer + 1) % fileServerHosts.size();
        return server;
    }

    private void upload(String host, String user, String password, int port, InputStream in, String remotePath) throws JSchException, SftpException {
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
