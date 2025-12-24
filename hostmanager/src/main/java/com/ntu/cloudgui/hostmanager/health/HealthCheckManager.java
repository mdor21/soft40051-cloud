package com.ntu.cloudgui.hostmanager.health;

import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import com.ntu.cloudgui.hostmanager.scaling.ScalingEventPublisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages the health checks for the containers.
 */
public class HealthCheckManager {

    private static final Logger logger = LogManager.getLogger(HealthCheckManager.class);

    private final ContainerManager containerManager;
    private final DockerCommandExecutor dockerExecutor;
    private final ScalingEventPublisher eventPublisher;

    public HealthCheckManager(ContainerManager containerManager, DockerCommandExecutor dockerExecutor, ScalingEventPublisher eventPublisher) {
        this.containerManager = containerManager;
        this.dockerExecutor = dockerExecutor;
        this.eventPublisher = eventPublisher;
    }

    public void performHealthCheck() {
        containerManager.getAllContainers().forEach(container -> {
            ProcessResult result = dockerExecutor.inspectContainer(container.getContainerName());
            boolean isHealthy = result.getExitCode() == 0 && result.getOutput().contains("\"Running\": true");
            containerManager.updateHealthStatus(container.getContainerName(), isHealthy);
            if (!isHealthy) {
                logger.warn("Container {} is unhealthy", container.getContainerName());
                // Optional: add logic to restart or remove unhealthy containers
            }
        });
    }

    public String getHealthStatus() {
        long healthyCount = containerManager.getAllContainers().stream().filter(c -> c.isHealthy()).count();
        return String.format("%d/%d healthy", healthyCount, containerManager.getAllContainers().size());
    }
}
