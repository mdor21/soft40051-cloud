package com.ntu.cloudgui.hostmanager.scaling;

import com.google.gson.Gson;
import com.ntu.cloudgui.hostmanager.mqtt.MqttConnectionManager;
import com.ntu.cloudgui.hostmanager.mqtt.MqttConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes scaling events to the MQTT broker.
 */
public class ScalingEventPublisher {

    private static final Logger logger = LogManager.getLogger(ScalingEventPublisher.class);

    private final MqttConnectionManager mqttConnectionManager;
    private final Gson gson;

    public ScalingEventPublisher(MqttConnectionManager mqttConnectionManager) {
        this.mqttConnectionManager = mqttConnectionManager;
        this.gson = new Gson();
    }

    public void publishScalingEvent(String action, String containerName) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("action", action);
            payload.put("containerName", containerName);
            String jsonPayload = gson.toJson(payload);
            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);
            mqttConnectionManager.publish(MqttConstants.TOPIC_SCALING_EVENTS, message);
        } catch (Exception e) {
            logger.error("Failed to publish scaling event", e);
        }
    }
}
