package com.ntu.cloudgui.aggservice.config;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SftpConnectionPool - Simple SFTP connection pool per host.
 */
@Component
public class SftpConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(SftpConnectionPool.class);

    private final SftpConfig sftpConfig;
    private final Map<String, SftpConnection> connections = new ConcurrentHashMap<>();

    public SftpConnectionPool(SftpConfig sftpConfig) {
        this.sftpConfig = sftpConfig;
        logger.info("SftpConnectionPool initialized");
    }

    public synchronized SftpConnection getConnection(String serverHost) throws Exception {
        SftpConnection existing = connections.get(serverHost);
        if (existing != null && existing.isConnected()) {
            existing.updateLastUsed();
            logger.debug("Reusing existing SFTP connection for host {}", serverHost);
            return existing;
        }

        logger.info("Creating new SFTP connection for host {}", serverHost);
        SftpConnection newConn = createNewConnection(serverHost);
        connections.put(serverHost, newConn);
        return newConn;
    }

    public synchronized void releaseConnection(String serverHost, SftpConnection connection) {
        if (connection == null) {
            return;
        }
        if (!connection.isConnected()) {
            logger.debug("Dropping closed SFTP connection for host {}", serverHost);
            connections.remove(serverHost);
        } else {
            connection.updateLastUsed();
        }
    }

    public synchronized void shutdown() {
        logger.info("Shutting down SftpConnectionPool...");
        for (Map.Entry<String, SftpConnection> entry : connections.entrySet()) {
            try {
                entry.getValue().disconnect();
            } catch (Exception e) {
                logger.warn("Error closing SFTP connection for host {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        connections.clear();
        logger.info("SftpConnectionPool shutdown complete");
    }

    private SftpConnection createNewConnection(String serverHost) throws Exception {
        String username = sftpConfig.getUsername();
        int port = sftpConfig.getPort();
        String password = sftpConfig.getPassword();
        int timeout = sftpConfig.getConnectionTimeout();

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, serverHost, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(timeout);

        Channel channel = session.openChannel("sftp");
        channel.connect(timeout);
        ChannelSftp sftpChannel = (ChannelSftp) channel;

        return new SftpConnection(session, sftpChannel, serverHost);
    }
}
