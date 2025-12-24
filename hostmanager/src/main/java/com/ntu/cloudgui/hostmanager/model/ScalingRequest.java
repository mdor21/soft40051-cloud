package com.ntu.cloudgui.hostmanager.model;

/**
 * Represents a scaling request received from the MQTT broker.
 */
public class ScalingRequest {

    private final String action;
    private final int count;

    public ScalingRequest(String action, int count) {
        this.action = action;
        this.count = count;
    }

    public String getAction() {
        return action;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ScalingRequest{" +
                "action='" + action + '\'' +
                ", count=" + count +
                '}';
    }
}
