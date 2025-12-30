package com.ntu.cloudgui.cloudlb;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Periodically inspects RequestQueue size and publishes
 * scaling commands to HostManager over MQTT.
 */
public class ScalingService {

    private final RequestQueue requestQueue;
    private final MqttClient client;
    private final String topic;
    private final Gson gson;

    public ScalingService(RequestQueue requestQueue,
                          String brokerUrl,
                          String clientId,
                          String topic) throws MqttException {
        this.requestQueue = requestQueue;
        this.client = new MqttClient(brokerUrl, clientId);
        this.topic = topic;
        this.client.connect();
        this.gson = new Gson();
        System.out.println("ScalingService connected to MQTT broker: " + brokerUrl);
    }

    /**
     * Check current queue size and decide whether to scale.
     * Call this periodically from MainLb using a ScheduledExecutor.
     */
    public void checkAndScale() {
        int queueSize = requestQueue.size();

        try {
            if (queueSize > 50) {
                // High load: request 4 file server containers
                publishScale("up", 4, queueSize);
            } else if (queueSize < 5) {
                // Low load: request 1 file server container
                publishScale("down", 1, queueSize);
            } else {
                // Normal load: maintain current state
                System.out.println("[Scaling] Queue size: " + queueSize + " - no action needed");
            }
        } catch (Exception e) {
            System.err.println("[Scaling] Error during scale check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void publishScale(String action, int value, int queueSize) throws MqttException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("count", value);
        String jsonPayload = gson.toJson(payload);

        System.out.println("[Scaling] Publishing: " + jsonPayload +
                " (queue size: " + queueSize + ")");

        MqttMessage message = new MqttMessage(jsonPayload.getBytes());
        message.setQos(1);
        message.setRetained(false);

        client.publish(topic, message);
        System.out.println("[Scaling] Message published to topic: " + topic);
    }
}
