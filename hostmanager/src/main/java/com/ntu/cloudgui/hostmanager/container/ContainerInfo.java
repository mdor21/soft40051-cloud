package com.ntu.cloudgui.hostmanager.container;

import java.time.LocalDateTime;

/**
 * Represents information about a container.
 */
public class ContainerInfo {

    private final String containerName;
    private boolean isHealthy;
    private LocalDateTime lastHealthCheck;

    public ContainerInfo(String containerName) {
        this.containerName = containerName;
        this.isHealthy = true;
        this.lastHealthCheck = LocalDateTime.now();
    }

    public String getContainerName() {
        return containerName;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public void setLastHealthCheck(LocalDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }
}
