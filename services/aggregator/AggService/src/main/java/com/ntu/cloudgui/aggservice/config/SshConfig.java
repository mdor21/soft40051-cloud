package com.ntu.cloudgui.aggservice.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * SshConfig - SSH/SFTP Connection Manager.
 *
 * Manages SSH connections to remote file servers for SFTP operations.
 */
@Configuration
public class SshConfig {

    private static final Logger logger = LoggerFactory.getLogger(SshConfig.class);

    private static final int CONNECTION_TIMEOUT = 10_000;  // 10 seconds
    private static final int KEXTIMEOUT = 60_000;          // 60 seconds

    private final JSch jsch;
    private final AppConfig appConfig;

    public SshConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.jsch = new JSch();
        logger.info("SSH Config initialized for host: {}:{}",
                appConfig.getSshHost(), appConfig.getSshPort());
    }

    /**
     * Create a new SSH session with authentication (not yet connected).
     */
    public Session createSession() throws JSchException {
        logger.debug("Creating SSH session to {}:{}",
                appConfig.getSshHost(), appConfig.getSshPort());

        try {
            Session session = jsch.getSession(
                    appConfig.getSshUser(),
                    appConfig.getSshHost(),
                    appConfig.getSshPort()
            );

            session.setPassword(appConfig.getSshPassword());
            session.setConfig("StrictHostKeyChecking", "no");      // testing only
            session.setConfig("PreferredAuthentications", "password");
            session.setConfig("ConnectTimeout", String.valueOf(CONNECTION_TIMEOUT));
            session.setConfig("kex_timeout", String.valueOf(KEXTIMEOUT));

            logger.debug("✓ SSH session created successfully");
            return session;
        } catch (JSchException e) {
            logger.error("✗ Failed to create SSH session", e);
            throw new JSchException("Failed to create SSH session: " + e.getMessage(), e);
        }
    }

    /**
     * Test SSH connection using current configuration.
     */
    public boolean testConnection() {
        Session session = null;
        try {
            logger.info("Testing SSH connection to {}:{}",
                    appConfig.getSshHost(), appConfig.getSshPort());

            session = createSession();
            session.connect(CONNECTION_TIMEOUT);

            logger.info("✓ SSH connection test successful");
            return true;
        } catch (JSchException e) {
            logger.error("✗ SSH connection test failed: {}", e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public JSch getJsch() {
        return jsch;
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    /**
     * Check that required SSH settings are present.
     */
    public boolean isConfigured() {
        return appConfig.getSshHost() != null && !appConfig.getSshHost().isEmpty()
                && appConfig.getSshUser() != null && !appConfig.getSshUser().isEmpty()
                && appConfig.getSshPassword() != null && !appConfig.getSshPassword().isEmpty()
                && appConfig.getSshPort() > 0;
    }

    public String getConnectionString() {
        return String.format("%s@%s:%d",
                appConfig.getSshUser(),
                appConfig.getSshHost(),
                appConfig.getSshPort());
    }
}
