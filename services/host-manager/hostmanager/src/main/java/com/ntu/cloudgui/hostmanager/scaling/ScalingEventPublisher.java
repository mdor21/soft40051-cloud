/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ntu.cloudgui.hostmanager.scaling;

/**
 * Publishes scaling-related events to MQTT
 */
public class ScalingEventPublisher {

    private static final Logger logger = LogManager.getLogger(ScalingEventPublisher.class);

    private MqttConnectionManager mqttConnectionManager;
    private Gson gson = new Gson();

    /**
     * Constructor
     */
    public ScalingEventPublisher(MqttConnectionManager mqttConnectionManager) {
        this.mqttConnectionManager = mqttConnectionManager;
        logger.info("ScalingEventPublisher initialized");
    }

    /**
     * Publish scale-up started event
     */
    public void publishScaleUpStarted(int quantity) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SCALE_UP_STARTED");
        event.put("quantity", quantity);
        event.put("timestamp", System.currentTimeMillis());

        publishEvent(MqttConstants.TOPIC_EVENTS, event);
    }

    /**
     * Publish scale-up complete event
     */
    public void publishScaleUpComplete(List<String> containerNames) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SCALE_UP_COMPLETE");
        event.put("containerNames", containerNames);
        event.put("quantity", containerNames.size());
        event.put("timestamp", System.currentTimeMillis());

        publishEvent(MqttConstants.TOPIC_STATUS, event);
    }

    /**
     * Publish scale-down started event
     */
    public void publishScaleDownStarted(int quantity) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SCALE_DOWN_STARTED");
        event.put("quantity", quantity);
        event.put("timestamp", System.currentTimeMillis());

        publishEvent(MqttConstants.TOPIC_EVENTS, event);
    }

    /**
     * Publish scale-down complete event
     */
    public void publishScaleDownComplete(List<String> containerNames) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SCALE_DOWN_COMPLETE");
        event.put("containerNames", containerNames);
        event.put("quantity", containerNames.size());
        event.put("timestamp", System.currentTimeMillis());

        publishEvent(MqttConstants.TOPIC_STATUS, event);
    }

    /**
     * Publish container started event
     */
    public void publishContainerStarted(String containerName, int port) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CONTAINER_STARTED");
        event.put("containerName", containerName);
        event.put("port", port);
        event.put("timestamp", System.currentTimeMillis());

        publishEvent(MqttConstants.TOPIC_EVENTS, event);
    }

    /**
     * Publish container stopped event
     */
    public void publishContainerStopped(String containerName) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CONTAINER_STOPPED");
        event.put("containerName", containerName);
        event.put("timestamp", System.currentTimeMillis());

        publishEvent(MqttConstants.TOPIC_EVENTS, event);
    }

    /**
     * Publish health status
     */
    public void publishHealthStatus(List<ContainerInfo> containers) {
        Map<String, Object> status = new HashMap<>();
        status.put("eventType", "HEALTH_STATUS");
        status.put("activeContainers", containers.size());
        status.put("healthyContainers",
                   containers.stream().filter(ContainerInfo::isHealthy).count());
        status.put("timestamp", System.currentTimeMillis());
        status.put("containers", containers.stream()
            .map(c -> Map.of("name", c.getContainerName(), "status", c.getStatus()))
            .toList());

        publishEvent(MqttConstants.TOPIC_STATUS, status);
    }

    /**
     * Generic event publisher
     */
    private void publishEvent(String topic, Map<String, Object> event) {
        try {
            String jsonMessage = gson.toJson(event);

            boolean published = mqttConnectionManager.publish(topic, jsonMessage);

            if (published) {
                logger.debug("Event published to {}: {}", topic, event.get("eventType"));
            } else {
                logger.warn("Failed to publish event to topic: {}", topic);
            }

        } catch (Exception e) {
            logger.error("Error publishing event", e);
        }
    }
}