package com.ntu.cloudgui.hostmanager.config;

/**
 * Holds configuration for connecting to the MQTT broker
 * and subscribing to scaling topics from the Load Balancer.
 */
public class MqttConfig {

    private final String brokerUrl;
    private final String clientId;
    private final String scaleTopic;
    private final int qos;

    public MqttConfig(String brokerUrl, String clientId, String scaleTopic, int qos) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.scaleTopic = scaleTopic;
        this.qos = qos;
    }

    public static MqttConfig fromEnvOrDefaults() {
        String broker = System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://mqtt-broker:1883");
        String topic  = System.getenv().getOrDefault("MQTT_SCALE_TOPIC", "lb/scale");
        String client = "hostmanager-" + System.currentTimeMillis();
        int qos       = 1;
        return new MqttConfig(broker, client, topic, qos);
    }

    public String getBrokerUrl() { return brokerUrl; }
    public String getClientId()  { return clientId; }
    public String getScaleTopic(){ return scaleTopic; }
    public int getQos()          { return qos; }
}
