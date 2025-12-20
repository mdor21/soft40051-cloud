package com.ntu.cloudgui.hostmanager.mqtt;

import com.ntu.cloudgui.hostmanager.config.MqttConfig;
import com.ntu.cloudgui.hostmanager.util.LogUtil;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * MQTT client wrapper for the Host Manager.
 *
 * Connects to the broker defined in {@link MqttConfig}, subscribes to the
 * scaling topic, and forwards every received message to {@link ScalingMessageHandler}.
 */
public class MqttClientService implements MqttCallback {

    private final MqttConfig config;
    private final ScalingMessageHandler handler;
    private MqttClient client;

    public MqttClientService(MqttConfig config, ScalingMessageHandler handler) {
        this.config = config;
        this.handler = handler;
    }

    /**
     * Connects to the MQTT broker and subscribes to the scaling topic.
     */
    public void connectAndSubscribe() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(config.getBrokerUrl(), config.getClientId(), persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.setCallback(this);
            client.connect(options);
            client.subscribe(config.getScaleTopic(), config.getQos());

            LogUtil.info("Connected to MQTT broker at " + config.getBrokerUrl()
                    + " and subscribed to topic " + config.getScaleTopic());
        } catch (MqttException e) {
            LogUtil.error("Failed to connect/subscribe to MQTT broker: " + e.getMessage(), e);
        }
    }

    // -------- MqttCallback implementation --------

    @Override
    public void connectionLost(Throwable cause) {
        String msg = (cause != null) ? cause.getMessage() : "unknown";
        LogUtil.error("MQTT connection lost: " + msg + " (auto‑reconnect enabled)", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        LogUtil.info("MQTT message arrived on topic " + topic + " payload: " + payload);
        handler.handleMessage(topic, payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Not used: this service only subscribes.
    }
}
