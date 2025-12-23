/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ntu.cloudgui.hostmanager.container;

/**
 * Thread-safe container registry and lifecycle management
 */
public class ContainerManager {
    
    private static final Logger logger = LogManager.getLogger(ContainerManager.class);
    
    private final ConcurrentHashMap<String, ContainerInfo> activeContainers;
    private int nextContainerId = 1;
    
    /**
     * Constructor
     */
    public ContainerManager() {
        this.activeContainers = new ConcurrentHashMap<>();
        logger.info("ContainerManager initialized");
    }
    
    /**
     * Add a container
     */
    public synchronized void addContainer(ContainerInfo container) {
        activeContainers.put(container.getContainerName(), container);
        logger.info("Container added: {} (ID: {})", 
                   container.getContainerName(), container.getContainerId());
    }
    
    /**
     * Remove a container
     */
    public synchronized void removeContainer(String containerName) {
        ContainerInfo removed = activeContainers.remove(containerName);
        if (removed != null) {
            logger.info("Container removed: {}", containerName);
        }
    }
    
    /**
     * Get container by name
     */
    public ContainerInfo getContainer(String containerName) {
        return activeContainers.get(containerName);
    }
    
    /**
     * Get all containers
     */
    public List<ContainerInfo> getAllContainers() {
        return new ArrayList<>(activeContainers.values());
    }
    
    /**
     * Get number of active containers
     */
    public int getContainerCount() {
        return activeContainers.size();
    }
    
    /**
     * Check if container exists
     */
    public boolean containerExists(String containerName) {
        return activeContainers.containsKey(containerName);
    }
    
    /**
     * Update container status
     */
    public synchronized void updateContainerStatus(String containerName, String status) {
        ContainerInfo container = activeContainers.get(containerName);
        if (container != null) {
            container.setStatus(status);
            logger.debug("Container status updated: {} -> {}", containerName, status);
        }
    }
    
    /**
     * Get healthy containers
     */
    public List<ContainerInfo> getHealthyContainers() {
        return activeContainers.values().stream()
            .filter(ContainerInfo::isHealthy)
            .toList();
    }
    
    /**
     * Get unhealthy containers
     */
    public List<ContainerInfo> getUnhealthyContainers() {
        return activeContainers.values().stream()
            .filter(c -> !c.isHealthy())
            .toList();
    }
    
    /**
     * Select container to stop (oldest first)
     */
    public ContainerInfo selectContainerToStop() {
        return activeContainers.values().stream()
            .min(Comparator.comparingLong(ContainerInfo::getCreatedTime))
            .orElse(null);
    }
    
    /**
     * Get next available port
     */
    public int getNextAvailablePort() {
        int basePort = 8000;
        int maxPort = basePort + activeContainers.size() + 1;
        return maxPort;
    }
    
    /**
     * Get next container ID
     */
    public synchronized int getNextContainerId() {
        return nextContainerId++;
    }
    
    /**
     * Clear all containers (for testing)
     */
    public synchronized void clearAll() {
        activeContainers.clear();
        logger.warn("All containers cleared");
    }
    
    /**
     * Get container summary
     */
    public String getSummary() {
        return String.format(
            "ContainerManager[total=%d, healthy=%d, unhealthy=%d]",
            getContainerCount(),
            getHealthyContainers().size(),
            getUnhealthyContainers().size()
        );
    }
}