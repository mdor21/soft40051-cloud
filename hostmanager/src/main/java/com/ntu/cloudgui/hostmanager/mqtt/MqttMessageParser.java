package com.ntu.cloudgui.hostmanager.mqtt;

import com.ntu.cloudgui.hostmanager.model.ScalingRequest;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

/**
 * Parses MQTT messages into ScalingRequest objects.
 */
public class MqttMessageParser {

    /**
     * Parses a JSON string into a ScalingRequest object.
     *
     * @param json The JSON string to parse.
     * @return A ScalingRequest object.
     */
    public ScalingRequest parse(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject jsonObject = reader.readObject();
            String action = jsonObject.getString("action");
            int count = jsonObject.getInt("count");
            return new ScalingRequest(action, count);
        }
    }
}
