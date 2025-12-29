package com.ntu.cloudgui.hostmanager.config;

/**
 * ConfigProperties - Configuration Property Definitions
 *
 * Central repository for all configuration property keys and default values.
 * This class ensures consistency and prevents typos in configuration keys.
 *
 * Organization: - Property key constants (used by ApplicationConfig) - Default
 * value constants (fallback values) - Helper methods for property generation
 *
 * Usage: String key = ConfigProperties.MQTT_BROKER_HOST; String defaultValue =
 * ConfigProperties.DEFAULT_MQTT_BROKER_HOST;
 *
 * @author SOFT40051 Coursework
 * @version 1.0
 */
public class ConfigProperties {

    // ==================== Environment ====================
    public static final String ENV = "app.env";
    public static final String DEFAULT_ENV = "dev";

    // ==================== MQTT Properties ====================
    // Property Keys
    public static final String MQTT_BROKER_HOST = "mqtt.broker.host";
    public static final String MQTT_BROKER_PORT = "mqtt.broker.port";
    public static final String MQTT_BROKER_USERNAME = "mqtt.broker.username";
    public static final String MQTT_BROKER_PASSWORD = "mqtt.broker.password";
    public static final String MQTT_CONNECTION_TIMEOUT = "mqtt.connection.timeout";
    public static final String MQTT_KEEP_ALIVE = "mqtt.keep.alive";
    public static final String MQTT_RECONNECT_INTERVAL = "mqtt.reconnect.interval";
    public static final String MQTT_RECONNECT_MAX_ATTEMPTS = "mqtt.reconnect.max.attempts";

    // Default Values
    public static final String DEFAULT_MQTT_BROKER_HOST = "localhost";
    public static final int DEFAULT_MQTT_BROKER_PORT = 1883;
    public static final String DEFAULT_MQTT_BROKER_USERNAME = "";
    public static final String DEFAULT_MQTT_BROKER_PASSWORD = "";
    public static final int DEFAULT_MQTT_CONNECTION_TIMEOUT = 30;
    public static final int DEFAULT_MQTT_KEEP_ALIVE = 60;
    public static final int DEFAULT_MQTT_RECONNECT_INTERVAL = 5;
    public static final int DEFAULT_MQTT_RECONNECT_MAX_ATTEMPTS = 3;

    // ==================== Docker Properties ====================
    // Property Keys
    public static final String DOCKER_HOST = "docker.host";
    public static final String DOCKER_PORT = "docker.port";
    public static final String DOCKER_IMAGE_NAME = "docker.image.name";
    public static final String DOCKER_SOCKET_PATH = "docker.socket.path";
    public static final String DOCKER_API_VERSION = "docker.api.version";
    public static final String DOCKER_TLS_VERIFY = "docker.tls.verify";

    // Default Values
    public static final String DEFAULT_DOCKER_HOST = "localhost";
    public static final int DEFAULT_DOCKER_PORT = 2375;
    public static final String DEFAULT_DOCKER_IMAGE_NAME = "ubuntu:latest";
    public static final String DEFAULT_DOCKER_SOCKET_PATH = "/var/run/docker.sock";
    public static final String DEFAULT_DOCKER_API_VERSION = "v1.40";
    public static final boolean DEFAULT_DOCKER_TLS_VERIFY = false;

    // ==================== Container Properties ====================
    // Property Keys
    public static final String CONTAINER_BASE_NAME = "container.base.name";
    public static final String CONTAINER_BASE_PORT = "container.base.port";
    public static final String CONTAINER_MAX_COUNT = "container.max.count";
    public static final String CONTAINER_MIN_COUNT = "container.min.count";
    public static final String CONTAINER_STARTUP_TIMEOUT = "container.startup.timeout";
    public static final String CONTAINER_SHUTDOWN_TIMEOUT = "container.shutdown.timeout";
    public static final String CONTAINER_MEMORY_LIMIT = "container.memory.limit";
    public static final String CONTAINER_CPU_LIMIT = "container.cpu.limit";
    public static final String CONTAINER_VOLUME_PATH = "container.volume.path";
    public static final String CONTAINER_NETWORK_MODE = "container.network.mode";

    // Default Values
    public static final String DEFAULT_CONTAINER_BASE_NAME = "fileserver-";
    public static final int DEFAULT_CONTAINER_BASE_PORT = 8000;
    public static final int DEFAULT_CONTAINER_MAX_COUNT = 10;
    public static final int DEFAULT_CONTAINER_MIN_COUNT = 2;
    public static final int DEFAULT_CONTAINER_STARTUP_TIMEOUT = 30;
    public static final int DEFAULT_CONTAINER_SHUTDOWN_TIMEOUT = 30;
    public static final String DEFAULT_CONTAINER_MEMORY_LIMIT = "512m";
    public static final String DEFAULT_CONTAINER_CPU_LIMIT = "0.5";
    public static final String DEFAULT_CONTAINER_VOLUME_PATH = "/data";
    public static final String DEFAULT_CONTAINER_NETWORK_MODE = "bridge";

    // ==================== Health Check Properties ====================
    // Property Keys
    public static final String HEALTH_CHECK_ENABLED = "health.check.enabled";
    public static final String HEALTH_CHECK_INTERVAL = "health.check.interval";
    public static final String HEALTH_CHECK_TIMEOUT = "health.check.timeout";
    public static final String HEALTH_CHECK_RETRY_COUNT = "health.check.retry.count";
    public static final String HEALTH_CHECK_ENDPOINT = "health.check.endpoint";
    public static final String HEALTH_CHECK_PORT = "health.check.port";

    // Default Values
    public static final boolean DEFAULT_HEALTH_CHECK_ENABLED = true;
    public static final int DEFAULT_HEALTH_CHECK_INTERVAL = 10;
    public static final int DEFAULT_HEALTH_CHECK_TIMEOUT = 5;
    public static final int DEFAULT_HEALTH_CHECK_RETRY_COUNT = 3;
    public static final String DEFAULT_HEALTH_CHECK_ENDPOINT = "/health";
    public static final int DEFAULT_HEALTH_CHECK_PORT = 8080;

    // ==================== Scaling Properties ====================
    // Property Keys
    public static final String SCALING_ENABLED = "scaling.enabled";
    public static final String SCALING_UP_THRESHOLD = "scaling.up.threshold";
    public static final String SCALING_DOWN_THRESHOLD = "scaling.down.threshold";
    public static final String SCALING_UP_COOLDOWN = "scaling.up.cooldown";
    public static final String SCALING_DOWN_COOLDOWN = "scaling.down.cooldown";
    public static final String SCALING_METRIC_TYPE = "scaling.metric.type";

    // Default Values
    public static final boolean DEFAULT_SCALING_ENABLED = true;
    public static final int DEFAULT_SCALING_UP_THRESHOLD = 80;
    public static final int DEFAULT_SCALING_DOWN_THRESHOLD = 20;
    public static final int DEFAULT_SCALING_UP_COOLDOWN = 60;
    public static final int DEFAULT_SCALING_DOWN_COOLDOWN = 300;
    public static final String DEFAULT_SCALING_METRIC_TYPE = "cpu";

    // ==================== Logging Properties ====================
    // Property Keys
    public static final String LOGGING_LEVEL = "logging.level";
    public static final String LOGGING_FILE_PATH = "logging.file.path";
    public static final String LOGGING_MAX_FILE_SIZE = "logging.max.file.size";
    public static final String LOGGING_MAX_BACKUP_INDEX = "logging.max.backup.index";
    public static final String LOGGING_PATTERN = "logging.pattern";

    // Default Values
    public static final String DEFAULT_LOGGING_LEVEL = "INFO";
    public static final String DEFAULT_LOGGING_FILE_PATH = "logs/hostmanager.log";
    public static final String DEFAULT_LOGGING_MAX_FILE_SIZE = "10MB";
    public static final int DEFAULT_LOGGING_MAX_BACKUP_INDEX = 5;
    public static final String DEFAULT_LOGGING_PATTERN = "%d{ISO8601} [%t] %-5p %c{1} - %m%n";

    // ==================== Database Properties ====================
    // Property Keys
    public static final String DB_SQLITE_ENABLED = "db.sqlite.enabled";
    public static final String DB_SQLITE_PATH = "db.sqlite.path";
    public static final String DB_MYSQL_HOST = "db.mysql.host";
    public static final String DB_MYSQL_PORT = "db.mysql.port";
    public static final String DB_MYSQL_DATABASE = "db.mysql.database";
    public static final String DB_MYSQL_USERNAME = "db.mysql.username";
    public static final String DB_MYSQL_PASSWORD = "db.mysql.password";
    public static final String DB_MYSQL_POOL_SIZE = "db.mysql.pool.size";

    // Default Values
    public static final boolean DEFAULT_DB_SQLITE_ENABLED = true;
    public static final String DEFAULT_DB_SQLITE_PATH = "data/hostmanager.db";
    public static final String DEFAULT_DB_MYSQL_HOST = "localhost";
    public static final int DEFAULT_DB_MYSQL_PORT = 3306;
    public static final String DEFAULT_DB_MYSQL_DATABASE = "hostmanager";
    public static final String DEFAULT_DB_MYSQL_USERNAME = "hostmanager";
    public static final String DEFAULT_DB_MYSQL_PASSWORD = "";
    public static final int DEFAULT_DB_MYSQL_POOL_SIZE = 10;

    // ==================== Application Properties ====================
    // Property Keys
    public static final String APP_NAME = "app.name";
    public static final String APP_VERSION = "app.version";
    public static final String APP_DESCRIPTION = "app.description";
    public static final String APP_PORT = "app.port";
    public static final String APP_SHUTDOWN_TIMEOUT = "app.shutdown.timeout";

    // Default Values
    public static final String DEFAULT_APP_NAME = "HostManager";
    public static final String DEFAULT_APP_VERSION = "1.0.0";
    public static final String DEFAULT_APP_DESCRIPTION = "Docker Container Orchestration & Auto-Scaling Manager";
    public static final int DEFAULT_APP_PORT = 8000;
    public static final int DEFAULT_APP_SHUTDOWN_TIMEOUT = 30;

    // ==================== MQTT Topics ====================
    public static final String MQTT_TOPIC_SCALING_REQUESTS = "loadbalancer/scaling/requests";
    public static final String MQTT_TOPIC_SCALING_RESPONSES = "hostmanager/scaling/responses";
    public static final String MQTT_TOPIC_STATUS = "hostmanager/status";
    public static final String MQTT_TOPIC_HEALTH = "hostmanager/health";
    public static final String MQTT_TOPIC_METRICS = "hostmanager/metrics";
    public static final String MQTT_TOPIC_EVENTS = "hostmanager/events";

    // ==================== Helper Methods ====================
    /**
     * Get property key for environment-specific override
     *
     * @param baseKey The base property key
     * @param env The environment name
     * @return Property key with environment suffix
     */
    public static String getEnvSpecificKey(String baseKey, String env) {
        if (env == null || env.isEmpty() || env.equals(DEFAULT_ENV)) {
            return baseKey;
        }
        return baseKey + "." + env;
    }

    /**
     * Get MQTT topic for a given resource
     *
     * @param resourceType Type of resource (scaling, health, status, etc.)
     * @return Full MQTT topic path
     */
    public static String getMqttTopic(String resourceType) {
        return "hostmanager/" + resourceType;
    }

    /**
     * Validate that scaling thresholds are sensible
     *
     * @param upThreshold Scaling up threshold percentage
     * @param downThreshold Scaling down threshold percentage
     * @return true if thresholds are valid
     */
    public static boolean validateScalingThresholds(int upThreshold, int downThreshold) {
        return upThreshold > downThreshold
                && upThreshold > 0
                && downThreshold >= 0
                && upThreshold <= 100
                && downThreshold <= 100;
    }

    /**
     * Validate that container counts are sensible
     *
     * @param minCount Minimum container count
     * @param maxCount Maximum container count
     * @return true if counts are valid
     */
    public static boolean validateContainerCounts(int minCount, int maxCount) {
        return minCount > 0
                && maxCount >= minCount
                && minCount <= 100
                && maxCount <= 100;
    }

    /**
     * Validate that timeout values are sensible
     *
     * @param timeout Timeout in seconds
     * @return true if timeout is valid
     */
    public static boolean validateTimeout(int timeout) {
        return timeout > 0 && timeout <= 3600; // 0-1 hour
    }

    /**
     * Get memory limit in bytes
     *
     * @param memoryString Memory specification (e.g., "512m", "1g")
     * @return Memory in bytes, or -1 if invalid
     */
    public static long parseMemoryLimit(String memoryString) {
        if (memoryString == null || memoryString.isEmpty()) {
            return -1;
        }

        memoryString = memoryString.toLowerCase().trim();
        long multiplier = 1;

        if (memoryString.endsWith("k")) {
            multiplier = 1024;
            memoryString = memoryString.substring(0, memoryString.length() - 1);
        } else if (memoryString.endsWith("m")) {
            multiplier = 1024 * 1024;
            memoryString = memoryString.substring(0, memoryString.length() - 1);
        } else if (memoryString.endsWith("g")) {
            multiplier = 1024 * 1024 * 1024;
            memoryString = memoryString.substring(0, memoryString.length() - 1);
        }

        try {
            long value = Long.parseLong(memoryString);
            return value * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parse CPU limit as double
     *
     * @param cpuString CPU specification (e.g., "0.5", "2.0")
     * @return CPU limit as double, or -1 if invalid
     */
    public static double parseCpuLimit(String cpuString) {
        if (cpuString == null || cpuString.isEmpty()) {
            return -1;
        }

        try {
            double value = Double.parseDouble(cpuString.trim());
            if (value > 0 && value <= 64) { // Reasonable limit
                return value;
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Get all MQTT topics as array
     */
    public static String[] getAllMqttTopics() {
        return new String[]{
            MQTT_TOPIC_SCALING_REQUESTS,
            MQTT_TOPIC_SCALING_RESPONSES,
            MQTT_TOPIC_STATUS,
            MQTT_TOPIC_HEALTH,
            MQTT_TOPIC_METRICS,
            MQTT_TOPIC_EVENTS
        };
    }

    /**
     * Get all property keys as array
     */
    public static String[] getAllPropertyKeys() {
        return new String[]{
            ENV,
            MQTT_BROKER_HOST,
            MQTT_BROKER_PORT,
            DOCKER_HOST,
            DOCKER_PORT,
            DOCKER_IMAGE_NAME,
            CONTAINER_BASE_NAME,
            CONTAINER_MAX_COUNT,
            CONTAINER_MIN_COUNT,
            HEALTH_CHECK_INTERVAL,
            SCALING_ENABLED,
            SCALING_UP_THRESHOLD,
            SCALING_DOWN_THRESHOLD,
            LOGGING_LEVEL,
            LOGGING_FILE_PATH
        };
    }

    // Prevent instantiation
    private ConfigProperties() {
        throw new AssertionError("ConfigProperties cannot be instantiated");
    }
}
