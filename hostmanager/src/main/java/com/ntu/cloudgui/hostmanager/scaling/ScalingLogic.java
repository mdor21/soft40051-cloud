package com.ntu.cloudgui.hostmanager.scaling;

import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.IntStream;

/**
 * Handles the logic for scaling containers up and down.
 */
public class ScalingLogic {

    private static final Logger logger = LogManager.getLogger(ScalingLogic.class);
    private static final String CONTAINER_BASE_NAME = "soft40051-files-container";
    private static final String IMAGE_NAME = "pedrombmachado/simple-ssh-container:base";
    private static final int MIN_PORT = 4848;
    private static final int MAX_CONTAINERS = 4;

    private final ContainerManager containerManager;
    private final DockerCommandExecutor dockerExecutor;
    private ScalingEventPublisher eventPublisher;

    public ScalingLogic(ContainerManager containerManager, DockerCommandExecutor dockerExecutor) {
        this.containerManager = containerManager;
        this.dockerExecutor = dockerExecutor;
    }

    public void setEventPublisher(ScalingEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handles a scale-up request.
     *
     * @param count The number of containers to add.
     */
    public void handleScaleUp(int count) {
        logger.info("Handling scale-up request for {} containers", count);
        IntStream.range(1, count + 1)
                .mapToObj(i -> CONTAINER_BASE_NAME + i)
                .filter(containerName -> !dockerExecutor.containerExists(containerName))
                .forEach(this::startContainer);
    }

    /**
     * Handles a scale-down request.
     *
     * @param count The number of containers to remove.
     */
    public void handleScaleDown(int count) {
        logger.info("Handling scale-down request for {} containers", count);
        IntStream.range(1, count + 1)
                .mapToObj(i -> CONTAINER_BASE_NAME + i)
                .filter(dockerExecutor::containerExists)
                .forEach(this::stopContainer);
    }

    private void startContainer(String containerName) {
        int containerNumber = Integer.parseInt(containerName.substring(CONTAINER_BASE_NAME.length()));
        int port = MIN_PORT + containerNumber - 1;
        ProcessResult result = dockerExecutor.runContainer(containerName, port, IMAGE_NAME);
        if (result.getExitCode() == 0) {
            containerManager.addContainer(containerName);
            eventPublisher.publishScalingEvent("up", containerName);
        }
    }

    private void stopContainer(String containerName) {
        ProcessResult result = dockerExecutor.stopContainer(containerName);
        if (result.getExitCode() == 0) {
            containerManager.removeContainer(containerName);
            eventPublisher.publishScalingEvent("down", containerName);
        }
    }
}
