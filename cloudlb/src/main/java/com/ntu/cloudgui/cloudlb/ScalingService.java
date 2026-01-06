package com.ntu.cloudgui.cloudlb;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Periodically inspects RequestQueue size and publishes
 * scaling commands to HostManager over MQTT.
 * Handles MQTT connection failures gracefully with retries.
 */
public class ScalingService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final RequestQueue requestQueue;
    private final MqttClient client;
    private final String brokerUrl;
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
                          int scaleDownThreshold) {
        this.requestQueue = requestQueue;
        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.scaleUpThreshold = scaleUpThreshold;
        this.scaleDownThreshold = scaleDownThreshold;
        this.gson = new Gson();

        MqttClient tempClient = null;
        try {
            tempClient = new MqttClient(brokerUrl, clientId);
        } catch (MqttException e) {
            System.err.printf("[%s] [SCALING] FATAL: Could not create MQTT client for %s: %s. Scaling will be disabled.%n",
                LocalDateTime.now().format(TIME_FORMAT), brokerUrl, e.getMessage());
        }
        this.client = tempClient;

        System.out.printf("[%s] [SCALING] Service initialized for MQTT broker: %s (thresholds: up=%d, down=%d)%n",
            LocalDateTime.now().format(TIME_FORMAT), brokerUrl, scaleUpThreshold, scaleDownThreshold);
    }

    /**
     * Ensures the MQTT client is connected. If not, attempts to reconnect.
     */
    private void ensureConnected() {
        if (client != null && !client.isConnected()) {
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            try {
                System.out.printf("[%s] [SCALING] MQTT client not connected. Attempting to connect to %s...%n", timestamp, brokerUrl);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                connOpts.setAutomaticReconnect(true); // Let the Paho library handle reconnects
                connOpts.setConnectionTimeout(5); // 5-second timeout
                client.connect(connOpts);
                System.out.printf("[%s] [SCALING] Successfully connected to MQTT broker.%n", LocalDateTime.now().format(TIME_FORMAT));
            } catch (MqttException e) {
                // This is expected if the broker is not ready, so don't print a scary stack trace.
                System.err.printf("[%s] [SCALING] Failed to connect to MQTT broker: %s%n", timestamp, e.getMessage());
            }
        }
    }

    /**
     * Check current queue size and decide whether to scale.
     * Call this periodically from MainLb using a ScheduledExecutor.
     */
    public void checkAndScale() {
        if (this.client == null) {
            return; // Service was not initialized correctly.
        }

        ensureConnected(); // Make sure we are connected before checking

        if (!client.isConnected()) {
             // Silently return if not connected, ensureConnected will have logged the error.
            return;
        }

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
        } catch (MqttException e) {
            // Log and disconnect so ensureConnected will try again next time.
            System.err.printf("[%s] [SCALING] Error during scale check: %s. Disconnecting.%n", timestamp, e.getMessage());
            try {
                client.disconnect();
            } catch (MqttException disconnectException) {
                // Ignore
            }
        } catch (Exception e) {
            System.err.printf("[%s] [SCALING] Unexpected error during scale check: %s%n", timestamp, e.getMessage());
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
