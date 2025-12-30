package com.ntu.cloudgui.hostmanager.mqtt;

import com.ntu.cloudgui.hostmanager.model.ScalingRequest;
import org.junit.Test;
import static org.junit.Assert.*;

public class MqttMessageParserTest {

    @Test
    public void testParse() {
        MqttMessageParser parser = new MqttMessageParser();
        String json = "{\"action\":\"up\",\"count\":1}";
        ScalingRequest request = parser.parse(json);
        assertEquals("up", request.getAction());
        assertEquals(1, request.getCount());
    }
}
