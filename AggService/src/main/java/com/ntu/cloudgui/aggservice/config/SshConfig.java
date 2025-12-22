package com.ntu.cloudgui.aggservice.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SshConfig - SSH/SFTP Connection Manager
 * 
 * Manages SSH connections to remote file servers for SFTP operations.
 * Provides session creation with authentication and connection pooling.
 * 
 * Responsibilities:
 * - Initialize JSch SSH client
 * - Create authenticated SSH sessions
 * - Manage SSH connection properties
 * - Handle connection timeouts and retries
 */
public class SshConfig {
    private static final Logger logger = LoggerFactory.getLogger(SshConfig.class);
    
    private final JSch jsch;
    private final AppConfig appConfig;
    
    // SSH Connection Timeouts (milliseconds)
    private static final int CONNECTION_TIMEOUT = 10000;  // 10 seconds
    private static final int KEXTIMEOUT = 60000;          // 60 seconds for key exchange
    
    /**
     * Constructor - Initialize SSH configuration
     * 
     * @param appConfig Application configuration with SSH credentials
     */
    public SshConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.jsch = new JSch();
        
        logger.info("SSH Config initialized for host: {}:{}", 
                   appConfig.getSshHost(), appConfig.getSshPort());
    }
    
    /**
     * Create a new SSH session with authentication
     * 
     * Opens an authenticated SSH connection to the configured server.
     * Session must be connected by caller.
     * 
     * Configuration:
     * - Host key checking disabled (for simplicity, enable in production)
     * - Connection timeout: 10 seconds
     * - Server key exchange timeout: 60 seconds
     * - Password authentication
     * 
     * Usage:
     * <pre>
     * Session session = sshConfig.createSession();
     * try {
     *     session.connect(10000);
     *     // Use session...
     * } finally {
     *     if (session != null && session.isConnected()) {
     *         session.disconnect();
     *     }
     * }
     * </pre>
     * 
     * @return Configured but unconnected SSH Session
     * @throws JSchException if session creation fails
     */
    public Session createSession() throws JSchException {
        logger.debug("Creating SSH session to {}:{}", 
                    appConfig.getSshHost(), appConfig.getSshPort());
        
        try {
            // Create new session with credentials
            Session session = jsch.getSession(
                appConfig.getSshUser(),
                appConfig.getSshHost(),
                appConfig.getSshPort()
            );
            
            // Set password authentication
            session.setPassword(appConfig.getSshPassword());
            
            // SSH Security Configuration
            session.setConfig("StrictHostKeyChecking", "no");    // For testing only
            session.setConfig("PreferredAuthentications", "password");
            
            // Connection Timeouts
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
     * Test SSH connection
     * 
     * Attempts to establish a connection to verify SSH credentials are valid.
     * Connection is closed after test.
     * 
     * @return true if connection successful, false otherwise
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
    
    /**
     * Get JSch instance
     * 
     * Advanced usage - get underlying JSch for custom configuration.
     * 
     * @return JSch SSH client instance
     */
    public JSch getJsch() {
        return jsch;
    }
    
    /**
     * Get application configuration
     * 
     * @return AppConfig instance
     */
    public AppConfig getAppConfig() {
        return appConfig;
    }
    
    /**
     * Validate SSH configuration
     * 
     * Checks that all required SSH settings are configured.
     * 
     * @return true if all required settings present
     */
    public boolean isConfigured() {
        return appConfig.getSshHost() != null && !appConfig.getSshHost().isEmpty() &&
               appConfig.getSshUser() != null && !appConfig.getSshUser().isEmpty() &&
               appConfig.getSshPassword() != null && !appConfig.getSshPassword().isEmpty() &&
               appConfig.getSshPort() > 0;
    }
    
    /**
     * Get connection details as string
     * 
     * @return Formatted connection string (user@host:port)
     */
    public String getConnectionString() {
        return String.format("%s@%s:%d", 
                           appConfig.getSshUser(),
                           appConfig.getSshHost(),
                           appConfig.getSshPort());
    }
}
