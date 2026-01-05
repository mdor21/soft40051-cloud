package com.ntu.cloudgui.hostmanager.scaling;

import com.ntu.cloudgui.hostmanager.container.ContainerInfo;
import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
     * Finds the next available container numbers and creates them.
     *
     * @param count The number of containers to add.
     */
    public void handleScaleUp(int count) {
        logger.info("Handling scale-up request for {} containers", count);
        Set<String> activeContainers = containerManager.getAllContainers().stream()
                .map(ContainerInfo::getContainerName)
                .collect(Collectors.toSet());
        long currentSize = activeContainers.size();

        if (currentSize >= MAX_CONTAINERS) {
            logger.warn("Cannot scale up, already at maximum capacity of {}", MAX_CONTAINERS);
            return;
        }

        long spaceAvailable = MAX_CONTAINERS - currentSize;
        long containersToStart = Math.min(count, spaceAvailable);

        IntStream.rangeClosed(1, MAX_CONTAINERS)
                .mapToObj(i -> CONTAINER_BASE_NAME + i)
                .filter(name -> !activeContainers.contains(name))
                .limit(containersToStart)
                .forEach(this::startContainer);
    }

    /**
     * Handles a scale-down request.
     * Removes the highest-numbered containers first.
     *
     * @param count The number of containers to remove.
     */
    public void handleScaleDown(int count) {
        logger.info("Handling scale-down request for {} containers", count);
        if (count <= 0) return;

        List<String> containersToStop = containerManager.getAllContainers().stream()
                .map(ContainerInfo::getContainerName)
                .sorted(Comparator.comparingInt(this::getContainerNumber).reversed())
                .limit(count)
                .collect(Collectors.toList());

        if (containersToStop.isEmpty()) {
            logger.info("No containers to scale down.");
            return;
        }

        logger.info("Identified containers to stop: {}", containersToStop);
        containersToStop.forEach(this::stopContainer);
    }

    private void startContainer(String containerName) {
        try {
            int containerNumber = getContainerNumber(containerName);
            int port = MIN_PORT + containerNumber - 1;
            ProcessResult result = dockerExecutor.runContainer(containerName, port, IMAGE_NAME);
            if (result.getExitCode() == 0) {
                containerManager.addContainer(containerName);
                if (eventPublisher != null) {
                    eventPublisher.publishScalingEvent("up", containerName);
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Could not parse container number from name: {}", containerName, e);
        }
    }

    private void stopContainer(String containerName) {
        ProcessResult result = dockerExecutor.stopContainer(containerName);
        if (result.getExitCode() == 0) {
            containerManager.removeContainer(containerName);
            if (eventPublisher != null) {
                eventPublisher.publishScalingEvent("down", containerName);
            }
        }
    }

    private int getContainerNumber(String containerName) {
        return Integer.parseInt(containerName.substring(CONTAINER_BASE_NAME.length()));
    }
}
