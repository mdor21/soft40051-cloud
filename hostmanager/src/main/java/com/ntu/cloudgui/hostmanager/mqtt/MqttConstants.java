package com.ntu.cloudgui.hostmanager.mqtt;

/**
 * Constants for MQTT communication.
 */
public class MqttConstants {

    public static final String MQTT_BROKER_HOST = "localhost";
    public static final int MQTT_BROKER_PORT = 1883;
    public static final String TOPIC_SCALING_REQUESTS = "traffic/progress";
    public static final String TOPIC_SCALING_EVENTS = "hm/scale/event";

    private MqttConstants() {
        // prevent instantiation
    }

    public static String getBrokerHost() {
        return getEnvOrDefault("MQTT_BROKER_HOST", MQTT_BROKER_HOST);
    }

    public static int getBrokerPort() {
        String rawPort = getEnvOrDefault("MQTT_BROKER_PORT", String.valueOf(MQTT_BROKER_PORT));
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException e) {
            return MQTT_BROKER_PORT;
        }
    }

    public static String getScalingRequestsTopic() {
        return getEnvOrDefault("MQTT_TOPIC", TOPIC_SCALING_REQUESTS);
    }

    public static String getScalingEventsTopic() {
        return getEnvOrDefault("MQTT_TOPIC_EVENTS", TOPIC_SCALING_EVENTS);
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
