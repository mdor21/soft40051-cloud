package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    // Database properties
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUser;
    private String dbPass;

    // AggService properties
    private int aggServicePort;
    private int threadPoolSize;

    // File processing properties
    private int chunkSize;
    private String encryptionKey; // Should be securely managed

    // File server properties
    private List<String> fileServerHosts;
    private int fileServerConnections;
    private boolean resetSchema;


    public Configuration() {
        loadProperties();
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.warn("application.properties not found, using environment variables and defaults.");
            } else {
                props.load(input);
            }
        } catch (Exception e) {
            logger.error("Error loading application.properties, will rely on environment variables and defaults.", e);
        }

        // Load database configuration
        dbHost = getProperty(props, "MYSQL_HOST", "lamp-server");
        dbPort = Integer.parseInt(getProperty(props, "MYSQL_PORT", "3306"));
        dbName = getProperty(props, "MYSQL_DATABASE", "dbtutorial");
        dbUser = getProperty(props, "MYSQL_USER", "admin");
        dbPass = getProperty(props, "MYSQL_PASSWORD", "admin");

        // Load AggService configuration
        aggServicePort = Integer.parseInt(getProperty(props, "SERVER_PORT", "9000"));
        threadPoolSize = Integer.parseInt(getProperty(props, "THREAD_POOL_SIZE", "10"));

        // Load file processing configuration
        chunkSize = Integer.parseInt(getProperty(props, "CHUNK_SIZE_BYTES", "1048576")); // 1MB default
        encryptionKey = getProperty(props, "ENCRYPTION_KEY", null);
        if (encryptionKey == null) {
            throw new IllegalStateException("ENCRYPTION_KEY environment variable not set.");
        }

        // Load file server configuration
        String fileServers = getProperty(props, "STORAGE_NODES", "soft40051-files-container1,soft40051-files-container2,soft40051-files-container3,soft40051-files-container4");
        fileServerHosts = Arrays.stream(fileServers.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());
        fileServerConnections = Integer.parseInt(getProperty(props, "FILE_SERVER_MAX_CONNECTIONS", "5"));

        // Load schema reset flag
        resetSchema = Boolean.parseBoolean(getProperty(props, "RESET_SCHEMA", "false"));

        logger.info("Configuration loaded successfully.");
    }

    private String getProperty(Properties props, String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            logger.info("Loaded '{}' from environment variable.", key);
            return value;
        }
        value = props.getProperty(key, defaultValue);
        logger.info("Loaded '{}' from properties file or default: {}", key, value);
        return value;
    }

    // Getters
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }
    public String getDbUser() { return dbUser; }
    public String getDbPass() { return dbPass; }
    public int getAggServicePort() { return aggServicePort; }
    public int getThreadPoolSize() { return threadPoolSize; }
    public int getChunkSize() { return chunkSize; }
    public String getEncryptionKey() { return encryptionKey; }
    public List<String> getFileServerHosts() { return fileServerHosts; }
    public int getFileServerConnections() { return fileServerConnections; }
    public boolean isResetSchema() { return resetSchema; }
}
