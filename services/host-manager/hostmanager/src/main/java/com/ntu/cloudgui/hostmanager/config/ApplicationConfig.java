package com.ntu.cloudgui.hostmanager.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ApplicationConfig - Centralized Configuration Management
 * 
 * Loads and manages all application configuration from:
 * - application.properties (default)
 * - application-{env}.properties (environment-specific)
 * - System properties (highest priority)
 * 
 * Provides typed getters with fallback to defaults.
 * 
 * Usage:
 *   ApplicationConfig config = new ApplicationConfig();
 *   String brokerHost = config.getMqttBrokerHost();
 *   int brokerPort = config.getMqttBrokerPort();
 * 
 * @author SOFT40051 Coursework
 * @version 1.0
 */
public class ApplicationConfig {
    
    private static final Logger logger = LogManager.getLogger(ApplicationConfig.class);
    
    private Properties properties;
    private String environment;
    
    // Configuration Keys (Constants)
    private static final String ENV_PROPERTY = "app.env";
    private static final String ENV_DEFAULT = "dev";
    
    // MQTT Configuration Keys
    private static final String MQTT_BROKER_HOST = "mqtt.broker.host";
    private static final String MQTT_BROKER_PORT = "mqtt.broker.port";
    private static final String MQTT_BROKER_USERNAME = "mqtt.broker.username";
    private static final String MQTT_BROKER_PASSWORD = "mqtt.broker.password";
    private static final String MQTT_CONNECTION_TIMEOUT = "mqtt.connection.timeout";
    private static final String MQTT_KEEP_ALIVE = "mqtt.keep.alive";
    
    // Docker Configuration Keys
    private static final String DOCKER_HOST = "docker.host";
    private static final String DOCKER_PORT = "docker.port";
    private static final String DOCKER_IMAGE = "docker.image.name";
    private static final String DOCKER_SOCKET = "docker.socket.path";
    
    // Container Configuration Keys
    private static final String CONTAINER_BASE_NAME = "container.base.name";
    private static final String CONTAINER_BASE_PORT = "container.base.port";
    private static final String CONTAINER_MAX_COUNT = "container.max.count";
    private static final String CONTAINER_MIN_COUNT = "container.min.count";
    private static final String CONTAINER_STARTUP_TIMEOUT = "container.startup.timeout";
    private static final String CONTAINER_SHUTDOWN_TIMEOUT = "container.shutdown.timeout";
    private static final String CONTAINER_MEMORY_LIMIT = "container.memory.limit";
    private static final String CONTAINER_CPU_LIMIT = "container.cpu.limit";
    
    // Health Check Configuration Keys
    private static final String HEALTH_CHECK_INTERVAL = "health.check.interval";
    private static final String HEALTH_CHECK_TIMEOUT = "health.check.timeout";
    private static final String HEALTH_CHECK_RETRY_COUNT = "health.check.retry.count";
    
    // Scaling Configuration Keys
    private static final String SCALING_ENABLED = "scaling.enabled";
    private static final String SCALING_UP_THRESHOLD = "scaling.up.threshold";
    private static final String SCALING_DOWN_THRESHOLD = "scaling.down.threshold";
    
    // Logging Configuration Keys
    private static final String LOGGING_LEVEL = "logging.level";
    private static final String LOGGING_FILE_PATH = "logging.file.path";
    
    // Default Values
    private static final String DEFAULT_MQTT_HOST = "localhost";
    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final String DEFAULT_MQTT_USERNAME = "";
    private static final String DEFAULT_MQTT_PASSWORD = "";
    private static final int DEFAULT_MQTT_TIMEOUT = 30;
    private static final int DEFAULT_MQTT_KEEP_ALIVE = 60;
    
    private static final String DEFAULT_DOCKER_HOST = "localhost";
    private static final int DEFAULT_DOCKER_PORT = 2375;
    private static final String DEFAULT_DOCKER_IMAGE = "ubuntu:latest";
    private static final String DEFAULT_DOCKER_SOCKET = "/var/run/docker.sock";
    
    private static final String DEFAULT_CONTAINER_BASE_NAME = "fileserver-";
    private static final int DEFAULT_CONTAINER_BASE_PORT = 8000;
    private static final int DEFAULT_CONTAINER_MAX_COUNT = 10;
    private static final int DEFAULT_CONTAINER_MIN_COUNT = 2;
    private static final int DEFAULT_CONTAINER_STARTUP_TIMEOUT = 30;
    private static final int DEFAULT_CONTAINER_SHUTDOWN_TIMEOUT = 30;
    private static final String DEFAULT_CONTAINER_MEMORY_LIMIT = "512m";
    private static final String DEFAULT_CONTAINER_CPU_LIMIT = "0.5";
    
    private static final int DEFAULT_HEALTH_CHECK_INTERVAL = 10;
    private static final int DEFAULT_HEALTH_CHECK_TIMEOUT = 5;
    private static final int DEFAULT_HEALTH_CHECK_RETRY_COUNT = 3;
    
    private static final boolean DEFAULT_SCALING_ENABLED = true;
    private static final int DEFAULT_SCALING_UP_THRESHOLD = 80;
    private static final int DEFAULT_SCALING_DOWN_THRESHOLD = 20;
    
    private static final String DEFAULT_LOGGING_LEVEL = "INFO";
    private static final String DEFAULT_LOGGING_FILE_PATH = "logs/hostmanager.log";
    
    /**
     * Constructor - Load configuration from properties files
     */
    public ApplicationConfig() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    /**
     * Load configuration with environment-specific overrides
     * 
     * Priority (highest to lowest):
     * 1. System properties (-D flags)
     * 2. Environment-specific properties (application-{env}.properties)
     * 3. Default properties (application.properties)
     */
    private void loadConfiguration() {
        try {
            // 1. Load default properties
            loadPropertiesFile("application.properties");
            logger.info("Loaded default configuration from application.properties");
            
            // 2. Get environment (default to 'dev')
            environment = getPropertyString(ENV_PROPERTY, ENV_DEFAULT);
            logger.info("Active environment: {}", environment);
            
            // 3. Load environment-specific properties
            String envPropertiesFile = "application-" + environment + ".properties";
            loadPropertiesFile(envPropertiesFile);
            logger.info("Loaded environment-specific configuration from {}", envPropertiesFile);
            
            // 4. Log configuration summary
            logConfigurationSummary();
            
        } catch (Exception e) {
            logger.warn("Error loading configuration, using defaults", e);
        }
    }
    
    /**
     * Load properties from a file
     * 
     * @param fileName Name of properties file in resources directory
     */
    private void loadPropertiesFile(String fileName) {
        try {
            String resourcePath = "src/main/resources/" + fileName;
            
            // Try resource path first
            if (Files.exists(Paths.get(resourcePath))) {
                try (InputStream input = new FileInputStream(resourcePath)) {
                    properties.load(input);
                    logger.debug("Loaded properties from {}", resourcePath);
                }
            } else {
                // Try classpath
                try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
                    if (input != null) {
                        properties.load(input);
                        logger.debug("Loaded properties from classpath: {}", fileName);
                    } else {
                        logger.debug("Properties file not found: {}", fileName);
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Could not load properties file: {}", fileName, e);
        }
    }
    
    /**
     * Get a string property with fallback to default
     */
    private String getPropertyString(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get an integer property with fallback to default
     */
    private int getPropertyInt(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer property: {} = {}", key, value);
            }
        }
        
        String propValue = properties.getProperty(key);
        if (propValue != null) {
            try {
                return Integer.parseInt(propValue);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer property: {} = {}", key, propValue);
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Get a boolean property with fallback to default
     */
    private boolean getPropertyBoolean(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        
        String propValue = properties.getProperty(key);
        if (propValue != null) {
            return Boolean.parseBoolean(propValue);
        }
        
        return defaultValue;
    }
    
    // ==================== MQTT Configuration ====================
    
    public String getMqttBrokerHost() {
        return getPropertyString(MQTT_BROKER_HOST, DEFAULT_MQTT_HOST);
    }
    
    public int getMqttBrokerPort() {
        return getPropertyInt(MQTT_BROKER_PORT, DEFAULT_MQTT_PORT);
    }
    
    public String getMqttBrokerUsername() {
        return getPropertyString(MQTT_BROKER_USERNAME, DEFAULT_MQTT_USERNAME);
    }
    
    public String getMqttBrokerPassword() {
        return getPropertyString(MQTT_BROKER_PASSWORD, DEFAULT_MQTT_PASSWORD);
    }
    
    public int getMqttConnectionTimeout() {
        return getPropertyInt(MQTT_CONNECTION_TIMEOUT, DEFAULT_MQTT_TIMEOUT);
    }
    
    public int getMqttKeepAlive() {
        return getPropertyInt(MQTT_KEEP_ALIVE, DEFAULT_MQTT_KEEP_ALIVE);
    }
    
    // ==================== Docker Configuration ====================
    
    public String getDockerHost() {
        return getPropertyString(DOCKER_HOST, DEFAULT_DOCKER_HOST);
    }
    
    public int getDockerPort() {
        return getPropertyInt(DOCKER_PORT, DEFAULT_DOCKER_PORT);
    }
    
    public String getDockerImageName() {
        return getPropertyString(DOCKER_IMAGE, DEFAULT_DOCKER_IMAGE);
    }
    
    public String getDockerSocketPath() {
        return getPropertyString(DOCKER_SOCKET, DEFAULT_DOCKER_SOCKET);
    }
    
    // ==================== Container Configuration ====================
    
    public String getContainerBaseName() {
        return getPropertyString(CONTAINER_BASE_NAME, DEFAULT_CONTAINER_BASE_NAME);
    }
    
    public int getContainerBasePort() {
        return getPropertyInt(CONTAINER_BASE_PORT, DEFAULT_CONTAINER_BASE_PORT);
    }
    
    public int getContainerMaxCount() {
        return getPropertyInt(CONTAINER_MAX_COUNT, DEFAULT_CONTAINER_MAX_COUNT);
    }
    
    public int getContainerMinCount() {
        return getPropertyInt(CONTAINER_MIN_COUNT, DEFAULT_CONTAINER_MIN_COUNT);
    }
    
    public int getContainerStartupTimeout() {
        return getPropertyInt(CONTAINER_STARTUP_TIMEOUT, DEFAULT_CONTAINER_STARTUP_TIMEOUT);
    }
    
    public int getContainerShutdownTimeout() {
        return getPropertyInt(CONTAINER_SHUTDOWN_TIMEOUT, DEFAULT_CONTAINER_SHUTDOWN_TIMEOUT);
    }
    
    public String getContainerMemoryLimit() {
        return getPropertyString(CONTAINER_MEMORY_LIMIT, DEFAULT_CONTAINER_MEMORY_LIMIT);
    }
    
    public String getContainerCpuLimit() {
        return getPropertyString(CONTAINER_CPU_LIMIT, DEFAULT_CONTAINER_CPU_LIMIT);
    }
    
    // ==================== Health Check Configuration ====================
    
    public int getHealthCheckInterval() {
        return getPropertyInt(HEALTH_CHECK_INTERVAL, DEFAULT_HEALTH_CHECK_INTERVAL);
    }
    
    public int getHealthCheckTimeout() {
        return getPropertyInt(HEALTH_CHECK_TIMEOUT, DEFAULT_HEALTH_CHECK_TIMEOUT);
    }
    
    public int getHealthCheckRetryCount() {
        return getPropertyInt(HEALTH_CHECK_RETRY_COUNT, DEFAULT_HEALTH_CHECK_RETRY_COUNT);
    }
    
    // ==================== Scaling Configuration ====================
    
    public boolean isScalingEnabled() {
        return getPropertyBoolean(SCALING_ENABLED, DEFAULT_SCALING_ENABLED);
    }
    
    public int getScalingUpThreshold() {
        return getPropertyInt(SCALING_UP_THRESHOLD, DEFAULT_SCALING_UP_THRESHOLD);
    }
    
    public int getScalingDownThreshold() {
        return getPropertyInt(SCALING_DOWN_THRESHOLD, DEFAULT_SCALING_DOWN_THRESHOLD);
    }
    
    // ==================== Logging Configuration ====================
    
    public String getLoggingLevel() {
        return getPropertyString(LOGGING_LEVEL, DEFAULT_LOGGING_LEVEL);
    }
    
    public String getLoggingFilePath() {
        return getPropertyString(LOGGING_FILE_PATH, DEFAULT_LOGGING_FILE_PATH);
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Get active environment
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Get MQTT broker URL
     */
    public String getMqttBrokerUrl() {
        return "tcp://" + getMqttBrokerHost() + ":" + getMqttBrokerPort();
    }
    
    /**
     * Get Docker API URL
     */
    public String getDockerApiUrl() {
        return "http://" + getDockerHost() + ":" + getDockerPort();
    }
    
    /**
     * Log configuration summary for debugging
     */
    private void logConfigurationSummary() {
        logger.info("========== Configuration Summary ==========");
        logger.info("Environment: {}", getEnvironment());
        logger.info("MQTT Broker: {} ({})", getMqttBrokerUrl(), 
                    getMqttBrokerUsername().isEmpty() ? "no auth" : "with auth");
        logger.info("Docker API: {}", getDockerApiUrl());
        logger.info("Container Image: {}", getDockerImageName());
        logger.info("Container Limits: min={}, max={}", getContainerMinCount(), getContainerMaxCount());
        logger.info("Health Check: interval={}s, timeout={}s, retries={}", 
                    getHealthCheckInterval(), getHealthCheckTimeout(), getHealthCheckRetryCount());
        logger.info("Scaling: enabled={}, up-threshold={}%, down-threshold={}%", 
                    isScalingEnabled(), getScalingUpThreshold(), getScalingDownThreshold());
        logger.info("Logging Level: {}", getLoggingLevel());
        logger.info("==========================================");
    }
    
    /**
     * Validate configuration - Check for required values
     */
    public boolean validate() {
        boolean valid = true;
        
        if (getMqttBrokerHost().isEmpty()) {
            logger.error("MQTT broker host is not configured");
            valid = false;
        }
        
        if (getContainerMinCount() > getContainerMaxCount()) {
            logger.error("Container min count ({}) cannot exceed max count ({})", 
                        getContainerMinCount(), getContainerMaxCount());
            valid = false;
        }
        
        if (getContainerMinCount() < 1) {
            logger.error("Container min count must be at least 1");
            valid = false;
        }
        
        if (getScalingUpThreshold() <= getScalingDownThreshold()) {
            logger.error("Scaling up threshold must be greater than down threshold");
            valid = false;
        }
        
        return valid;
    }
}