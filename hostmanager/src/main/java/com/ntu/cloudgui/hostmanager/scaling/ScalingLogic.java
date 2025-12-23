/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ntu.cloudgui.hostmanager.scaling;

/**
 * Core scaling logic - handles scale-up and scale-down operations
 */
public class ScalingLogic {
    
    private static final Logger logger = LogManager.getLogger(ScalingLogic.class);
    
    private ContainerManager containerManager;
    private DockerCommandExecutor dockerExecutor;
    private ScalingEventPublisher eventPublisher;
    
    private volatile boolean isScaling = false;
    
    /**
     * Constructor
     */
    public ScalingLogic(ContainerManager containerManager, 
                       DockerCommandExecutor dockerExecutor) {
        this.containerManager = containerManager;
        this.dockerExecutor = dockerExecutor;
        logger.info("ScalingLogic initialized");
    }
    
    /**
     * Handle scale-up request
     */
    public synchronized void handleScaleUp(int quantity) {
        if (isScaling) {
            logger.warn("Already scaling, ignoring scale-up request");
            return;
        }
        
        try {
            isScaling = true;
            
            logger.info("Scale-up requested: {} containers", quantity);
            
            // Validate request
            if (!validateScaleUp(quantity)) {
                logger.warn("Scale-up validation failed");
                return;
            }
            
            logger.info("Starting scale-up process");
            
            List<String> startedContainers = new ArrayList<>();
            
            // Start containers sequentially
            for (int i = 0; i < quantity; i++) {
                String containerName = startNewContainer();
                if (containerName != null) {
                    startedContainers.add(containerName);
                    logger.info("Started container {}/{}: {}", i + 1, quantity, containerName);
                } else {
                    logger.warn("Failed to start container {}/{}", i + 1, quantity);
                }
            }
            
            // Publish completion event
            if (!startedContainers.isEmpty() && eventPublisher != null) {
                eventPublisher.publishScaleUpComplete(startedContainers);
            }
            
            logger.info("Scale-up completed: {} containers started", startedContainers.size());
            
        } catch (Exception e) {
            logger.error("Error during scale-up", e);
        } finally {
            isScaling = false;
        }
    }
    
    /**
     * Handle scale-down request
     */
    public synchronized void handleScaleDown(int quantity) {
        if (isScaling) {
            logger.warn("Already scaling, ignoring scale-down request");
            return;
        }
        
        try {
            isScaling = true;
            
            logger.info("Scale-down requested: {} containers", quantity);
            
            // Validate request
            if (!validateScaleDown(quantity)) {
                logger.warn("Scale-down validation failed");
                return;
            }
            
            logger.info("Starting scale-down process");
            
            List<String> stoppedContainers = new ArrayList<>();
            
            // Stop containers sequentially
            for (int i = 0; i < quantity; i++) {
                ContainerInfo containerToStop = containerManager.selectContainerToStop();
                if (containerToStop != null) {
                    boolean stopped = stopContainer(containerToStop.getContainerName());
                    if (stopped) {
                        stoppedContainers.add(containerToStop.getContainerName());
                        logger.info("Stopped container {}/{}: {}", 
                                   i + 1, quantity, containerToStop.getContainerName());
                    }
                }
            }
            
            // Publish completion event
            if (!stoppedContainers.isEmpty() && eventPublisher != null) {
                eventPublisher.publishScaleDownComplete(stoppedContainers);
            }
            
            logger.info("Scale-down completed: {} containers stopped", stoppedContainers.size());
            
        } catch (Exception e) {
            logger.error("Error during scale-down", e);
        } finally {
            isScaling = false;
        }
    }
    
    /**
     * Validate scale-up request
     */
    private boolean validateScaleUp(int quantity) {
        if (quantity <= 0) {
            logger.warn("Invalid quantity: {}", quantity);
            return false;
        }
        
        int currentCount = containerManager.getContainerCount();
        int newCount = currentCount + quantity;
        
        if (newCount > DockerConstants.MAX_CONTAINERS) {
            logger.warn("Cannot scale up: would exceed max containers ({} + {} > {})",
                       currentCount, quantity, DockerConstants.MAX_CONTAINERS);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate scale-down request
     */
    private boolean validateScaleDown(int quantity) {
        if (quantity <= 0) {
            logger.warn("Invalid quantity: {}", quantity);
            return false;
        }
        
        int currentCount = containerManager.getContainerCount();
        int newCount = currentCount - quantity;
        
        if (newCount < DockerConstants.MIN_CONTAINERS) {
            logger.warn("Cannot scale down: would violate min containers ({} - {} < {})",
                       currentCount, quantity, DockerConstants.MIN_CONTAINERS);
            return false;
        }
        
        return true;
    }
    
    /**
     * Start a new container
     */
    private String startNewContainer() {
        try {
            int containerId = containerManager.getNextContainerId();
            String containerName = DockerConstants.BASE_CONTAINER_NAME + containerId;
            int port = DockerConstants.BASE_PORT + containerId;
            
            logger.debug("Starting new container: {} on port {}", containerName, port);
            
            // Execute docker run
            var result = dockerExecutor.runContainer(containerName, port, 
                                                     DockerConstants.IMAGE_NAME);
            
            if (result.getExitCode() == 0) {
                // Add to container manager
                ContainerInfo info = new ContainerInfo(containerName, port);
                info.setStatus("RUNNING");
                containerManager.addContainer(info);
                
                logger.info("Container started successfully: {}", containerName);
                return containerName;
            } else {
                logger.error("Failed to start container: {}", result.getError());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error starting container", e);
            return null;
        }
    }
    
    /**
     * Stop a container
     */
    private boolean stopContainer(String containerName) {
        try {
            logger.debug("Stopping container: {}", containerName);
            
            var result = dockerExecutor.stopContainer(containerName);
            
            if (result.getExitCode() == 0) {
                containerManager.removeContainer(containerName);
                logger.info("Container stopped successfully: {}", containerName);
                return true;
            } else {
                logger.error("Failed to stop container: {}", result.getError());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error stopping container", e);
            return false;
        }
    }
    
    /**
     * Set event publisher
     */
    public void setEventPublisher(ScalingEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Check if scaling is in progress
     */
    public boolean isScaling() {
        return isScaling;
    }
    
    /**
     * Get current metrics
     */
    public String getMetrics() {
        return String.format(
            "ScalingMetrics[active=%d, healthy=%d, max=%d, min=%d, scaling=%s]",
            containerManager.getContainerCount(),
            containerManager.getHealthyContainers().size(),
            DockerConstants.MAX_CONTAINERS,
            DockerConstants.MIN_CONTAINERS,
            isScaling
        );
    }
}