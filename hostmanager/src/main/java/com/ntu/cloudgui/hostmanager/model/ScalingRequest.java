package com.ntu.cloudgui.hostmanager.model;

/**
 * Represents a scaling request received from the MQTT broker.
 */
public class ScalingRequest {

    private final String action;
    private final int count;
    private final Integer nodeIndex;

    public ScalingRequest(String action, int count) {
        this.action = action;
        this.count = count;
        this.nodeIndex = null;
    }

    public ScalingRequest(String action, int count, Integer nodeIndex) {
        this.action = action;
        this.count = count;
        this.nodeIndex = nodeIndex;
    }

    public String getAction() {
        return action;
    }

    public int getCount() {
        return count;
    }

    public Integer getNodeIndex() {
        return nodeIndex;
    }

    @Override
    public String toString() {
        return "ScalingRequest{" +
                "action='" + action + '\'' +
                ", count=" + count +
                ", nodeIndex=" + nodeIndex +
                '}';
    }
}
