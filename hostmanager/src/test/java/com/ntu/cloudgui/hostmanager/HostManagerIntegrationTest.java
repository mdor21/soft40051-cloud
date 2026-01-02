package com.ntu.cloudgui.hostmanager;

import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class HostManagerIntegrationTest {

    // private HostManager hostManager;
    // private MqttClient mqttClient;
    // private DockerCommandExecutor dockerExecutor;

    // @Before
    // public void setUp() throws Exception {
    //     hostManager = new HostManager();
    //     hostManager.start();

    //     dockerExecutor = new DockerCommandExecutor();

    //     mqttClient = new MqttClient("tcp://localhost:1883", "test-client");
    //     mqttClient.connect();
    // }

    // @After
    // public void tearDown() throws Exception {
    //     mqttClient.disconnect();
    //     hostManager.stop();
    // }

    // @Test
    // public void testScaleUpAndDown() throws Exception {
    //     // Scale up
    //     String scaleUpMessage = "{\"action\":\"up\",\"count\":1}";
    //     mqttClient.publish("loadbalancer/scaling/requests", new MqttMessage(scaleUpMessage.getBytes()));
    //     Thread.sleep(5000); // Allow time for the container to start
    //     assertTrue(dockerExecutor.containerExists("soft40051-files-container1"));

    //     // Scale down
    //     String scaleDownMessage = "{\"action\":\"down\",\"count\":1}";
    //     mqttClient.publish("loadbalancer/scaling/requests", new MqttMessage(scaleDownMessage.getBytes()));
    //     Thread.sleep(5000); // Allow time for the container to stop
    //     assertTrue(!dockerExecutor.containerExists("soft40051-files-container1"));
    // }
}
