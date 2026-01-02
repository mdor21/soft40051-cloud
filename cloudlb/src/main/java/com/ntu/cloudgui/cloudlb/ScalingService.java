package com.ntu.cloudgui.cloudlb;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Periodically inspects RequestQueue size and publishes
 * scaling commands to HostManager over MQTT.
 */
public class ScalingService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final RequestQueue requestQueue;
    private final MqttClient client;
    private final String topic;
    private final Gson gson;
    private final int scaleUpThreshold;
    private final int scaleDownThreshold;
    private int lastPublishedAction = 0; // Track last action to avoid repetition

    public ScalingService(RequestQueue requestQueue,
                          String brokerUrl,
                          String clientId,
                          String topic,
                          int scaleUpThreshold,
                          int scaleDownThreshold) throws MqttException {
        this.requestQueue = requestQueue;
        this.client = new MqttClient(brokerUrl, clientId);
        this.topic = topic;
        this.scaleUpThreshold = scaleUpThreshold;
        this.scaleDownThreshold = scaleDownThreshold;
        this.client.connect();
        this.gson = new Gson();
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] [SCALING] Connected to MQTT broker: %s (thresholds: up=%d, down=%d)%n",
            timestamp, brokerUrl, scaleUpThreshold, scaleDownThreshold);
    }

    /**
     * Check current queue size and decide whether to scale.
     * Call this periodically from MainLb using a ScheduledExecutor.
     */
    public void checkAndScale() {
        int queueSize = requestQueue.size();
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);

        try {
            if (queueSize > scaleUpThreshold && lastPublishedAction != 1) {
                // High load: request scale up
                publishScale("up", 4, queueSize);
                lastPublishedAction = 1;
            } else if (queueSize < scaleDownThreshold && lastPublishedAction != -1) {
                // Low load: request scale down
                publishScale("down", 1, queueSize);
                lastPublishedAction = -1;
            } else if (queueSize >= scaleDownThreshold && queueSize <= scaleUpThreshold) {
                // Normal load: maintain current state
                if (lastPublishedAction != 0) {
                    System.out.printf("[%s] [SCALING] Queue size: %d - stable state maintained%n",
                        timestamp, queueSize);
                    lastPublishedAction = 0;
                }
            }
        } catch (Exception e) {
            System.err.printf("[%s] [SCALING] Error during scale check: %s%n", timestamp, e.getMessage());
            e.printStackTrace();
        }
    }

    private void publishScale(String action, int value, int queueSize) throws MqttException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("count", value);
        payload.put("queueSize", queueSize);
        String jsonPayload = gson.toJson(payload);

        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] [SCALING] Publishing SCALE-%s: %s%n",
            timestamp, action.toUpperCase(), jsonPayload);

        MqttMessage message = new MqttMessage(jsonPayload.getBytes());
        message.setQos(1);
        message.setRetained(false);

        client.publish(topic, message);
        System.out.printf("[%s] [SCALING] Message published to topic: %s%n", timestamp, topic);
    }
}
