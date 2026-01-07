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
        dbHost = getProperty(props, "DB_HOST", "lamp-server");
        dbPort = Integer.parseInt(getProperty(props, "DB_PORT", "3306"));
        dbName = getProperty(props, "DB_NAME", "soft40051_db");
        dbUser = getProperty(props, "DB_USER", "user");
        dbPass = getProperty(props, "DB_PASS", "password");

        // Load AggService configuration
        aggServicePort = Integer.parseInt(getProperty(props, "AGGSERVICE_PORT", "8080"));
        threadPoolSize = Integer.parseInt(getProperty(props, "THREAD_POOL_SIZE", "10"));

        // Load file processing configuration
        chunkSize = Integer.parseInt(getProperty(props, "CHUNK_SIZE_BYTES", "1048576")); // 1MB default
        encryptionKey = getProperty(props, "ENCRYPTION_KEY", "aVerySecretKey123"); // Default key for development ONLY

        // Load file server configuration
        String fileServers = getProperty(props, "FILE_SERVER_HOSTS", "soft40051-files-container1,soft40051-files-container2,soft40051-files-container3,soft40051-files-container4");
        fileServerHosts = Arrays.stream(fileServers.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());
        fileServerConnections = Integer.parseInt(getProperty(props, "FILE_SERVER_MAX_CONNECTIONS", "5"));

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
}
