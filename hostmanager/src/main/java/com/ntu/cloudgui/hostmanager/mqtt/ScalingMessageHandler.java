package com.ntu.cloudgui.hostmanager.mqtt;

import com.ntu.cloudgui.hostmanager.docker.DockerService;
import com.ntu.cloudgui.hostmanager.util.LogUtil;
import com.ntu.cloudgui.hostmanager.util.ScalingRequest;

public class ScalingMessageHandler {

    private final DockerService dockerService;

    public ScalingMessageHandler(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    public void handleMessage(String topic, String payload) {
        LogUtil.info("ScalingMessageHandler received on " + topic + ": " + payload);

        ScalingRequest request = ScalingRequest.parse(payload);
        ScalingRequest.Mode mode = request.getMode();

        switch (mode) {
            case SET:
                LogUtil.info("Scaling SET to " + request.getValue() + " containers");
                dockerService.ensureDesiredCount(request.getValue());
                break;

            case SCALEUP:
                LogUtil.info("Scaling UP by " + request.getValue());
                dockerService.scaleUp(request.getValue());
                break;

            case SCALEDOWN:
                LogUtil.info("Scaling DOWN by " + request.getValue());
                dockerService.scaleDown(request.getValue());
                break;

            case UNKNOWN:
            default:
                LogUtil.error("Unknown scaling mode for payload: " + payload, null);
                break;
        }
    }
}
