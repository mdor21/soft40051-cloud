package com.ntu.cloudgui.hostmanager.util;

/**
 * Represents a scaling request sent by the Load Balancer.
 *
 * Expected payload formats:
 *   "SET,3"       -> set total containers to 3
 *   "SCALEUP,1"   -> add 1 container
 *   "SCALEDOWN,2" -> remove 2 containers
 */
public class ScalingRequest {

    public enum Mode {
        SET,
        SCALEUP,
        SCALEDOWN,
        UNKNOWN
    }

    private final Mode mode;
    private final int value;

    public ScalingRequest(Mode mode, int value) {
        this.mode = mode;
        this.value = value;
    }

    public Mode getMode() {
        return mode;
    }

    public int getValue() {
        return value;
    }

    /**
     * Parse a raw payload into a ScalingRequest.
     *
     * @param payload string such as "SET,3"
     * @return parsed ScalingRequest (UNKNOWN,0 if invalid)
     */
    public static ScalingRequest parse(String payload) {
        if (payload == null || !payload.contains(",")) {
            return new ScalingRequest(Mode.UNKNOWN, 0);
        }

        try {
            String[] parts = payload.trim().split(",", 2);
            String cmd = parts[0].toUpperCase();
            int val = Integer.parseInt(parts[1].trim());

            return switch (cmd) {
                case "SET"       -> new ScalingRequest(Mode.SET, val);
                case "SCALEUP"   -> new ScalingRequest(Mode.SCALEUP, val);
                case "SCALEDOWN" -> new ScalingRequest(Mode.SCALEDOWN, val);
                default          -> new ScalingRequest(Mode.UNKNOWN, val);
            };
        } catch (Exception e) {
            return new ScalingRequest(Mode.UNKNOWN, 0);
        }
    }

    @Override
    public String toString() {
        return "ScalingRequest{" +
                "mode=" + mode +
                ", value=" + value +
                '}';
    }
}
