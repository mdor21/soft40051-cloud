package com.ntu.cloudgui.hostmanager.mqtt;

/**
 * Constants for MQTT communication.
 */
public class MqttConstants {

    public static final String MQTT_BROKER_HOST = "mqtt-broker";
    public static final int MQTT_BROKER_PORT = 1883;
    public static final String TOPIC_SCALING_REQUESTS = "lb/scale/request";
    public static final String TOPIC_SCALING_EVENTS = "hm/scale/event";

    private MqttConstants() {
        // prevent instantiation
    }
}
