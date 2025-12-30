package com.ntu.cloudgui.hostmanager.container;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the state of the containers.
 */
public class ContainerManager {

    private static final Logger logger = LogManager.getLogger(ContainerManager.class);

    private final ConcurrentHashMap<String, ContainerInfo> containers;

    public ContainerManager() {
        this.containers = new ConcurrentHashMap<>();
    }

    public void addContainer(String containerName) {
        containers.put(containerName, new ContainerInfo(containerName));
        logger.info("Container added: {}", containerName);
    }

    public void removeContainer(String containerName) {
        containers.remove(containerName);
        logger.info("Container removed: {}", containerName);
    }

    public ContainerInfo getContainer(String containerName) {
        return containers.get(containerName);
    }

    public List<ContainerInfo> getAllContainers() {
        return new ArrayList<>(containers.values());
    }

    public String getSummary() {
        return String.format("%d running", containers.size());
    }

    public void updateHealthStatus(String containerName, boolean isHealthy) {
        ContainerInfo containerInfo = containers.get(containerName);
        if (containerInfo != null) {
            containerInfo.setHealthy(isHealthy);
        }
    }

    public List<String> getUnhealthyContainers() {
        return containers.values().stream()
                .filter(c -> !c.isHealthy())
                .map(ContainerInfo::getContainerName)
                .collect(Collectors.toList());
    }

    public List<String> getIdleContainers(long idleThresholdSeconds) {
        // This is a placeholder for more complex logic
        return new ArrayList<>();
    }
}
