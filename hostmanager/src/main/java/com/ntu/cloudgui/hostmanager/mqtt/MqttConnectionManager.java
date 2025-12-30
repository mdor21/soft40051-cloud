package com.ntu.cloudgui.hostmanager.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Manages the connection to the MQTT broker.
 */
public class MqttConnectionManager {

    private static final Logger logger = LogManager.getLogger(MqttConnectionManager.class);

    private final String brokerUrl;
    private final String clientId;
    private IMqttClient client;

    public MqttConnectionManager(String host, int port, String clientId) {
        this.brokerUrl = "tcp://" + host + ":" + port;
        this.clientId = clientId;
    }

    public void connect() throws MqttException {
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        client.connect(options);
        logger.info("Connected to MQTT broker at {}", brokerUrl);
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        client.subscribe(topic, listener);
        logger.info("Subscribed to topic {}", topic);
    }

    public void publish(String topic, MqttMessage message) throws MqttException {
        client.publish(topic, message);
        logger.debug("Published message to topic {}", topic);
    }

    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            logger.info("Disconnected from MQTT broker");
        }
    }
}
