package com.ntu.cloudgui.hostmanager;

import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import com.ntu.cloudgui.hostmanager.health.HealthCheckManager;
import com.ntu.cloudgui.hostmanager.model.ScalingRequest;
import com.ntu.cloudgui.hostmanager.mqtt.MqttConnectionManager;
import com.ntu.cloudgui.hostmanager.mqtt.MqttConstants;
import com.ntu.cloudgui.hostmanager.mqtt.MqttMessageParser;
import com.ntu.cloudgui.hostmanager.scaling.ScalingEventPublisher;
import com.ntu.cloudgui.hostmanager.scaling.ScalingLogic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HostManager - Main Application Entry Point
 * 
 * Orchestrates Docker container management, auto-scaling, health checks, and MQTT communication
 * for the SOFT40051 Cloud Load Balancer coursework.
 * 
 * Responsibilities:
 * - Initialize all core managers (Docker, Container, MQTT, Scaling, Health)
 * - Manage container lifecycle (start, stop, restart, scale up/down)
 * - Monitor container health and trigger recovery
 * - Publish scaling events via MQTT for load balancer synchronization
 * - Graceful shutdown and cleanup
 * 
 * Architecture Flow:
 * Load Balancer → MQTT → HostManager → Docker → Containers
 *     (requests)      (messages)   (translates)  (executes) (managed)
 * 
 * @author SOFT40051 Coursework
 * @version 1.0
 */
public class HostManager {
    
    private static final Logger logger = LogManager.getLogger(HostManager.class);
    
    // Core managers
    private DockerCommandExecutor dockerExecutor;
    private ContainerManager containerManager;
    private MqttConnectionManager mqttConnectionManager;
    private ScalingLogic scalingLogic;
    private ScalingEventPublisher eventPublisher;
    private HealthCheckManager healthCheckManager;
    private MqttMessageParser mqttMessageParser;
    
    // Application state
    private volatile boolean isRunning = false;
    private ScheduledExecutorService healthCheckScheduler;
    private ExecutorService taskExecutor;
    
    /**
     * Constructor - Initializes all managers
     */
    public HostManager() {
        logger.info("========== HostManager Initialization Started ==========");
        initializeManagers();
        logger.info("========== HostManager Initialization Complete ==========");
    }
    
    /**
     * Initialize all core managers
     */
    private void initializeManagers() {
        try {
            // 1. Docker Operations Manager
            logger.info("Initializing Docker Command Executor...");
            this.dockerExecutor = new DockerCommandExecutor();
            verifyDockerInstallation();
            
            // 2. Container Registry & Lifecycle Manager
            logger.info("Initializing Container Manager...");
            this.containerManager = new ContainerManager();
            
            // 3. MQTT Communication Manager
            logger.info("Initializing MQTT Connection Manager...");
            this.mqttConnectionManager = new MqttConnectionManager(
                MqttConstants.MQTT_BROKER_HOST,
                MqttConstants.MQTT_BROKER_PORT,
                "hostmanager-" + UUID.randomUUID().toString().substring(0, 8)
            );
            this.mqttConnectionManager.connect();
            
            // 4. Scaling Logic Manager
            logger.info("Initializing Scaling Logic...");
            this.scalingLogic = new ScalingLogic(containerManager, dockerExecutor);
            
            // 5. Scaling Event Publisher
            logger.info("Initializing Scaling Event Publisher...");
            this.eventPublisher = new ScalingEventPublisher(mqttConnectionManager);
            this.scalingLogic.setEventPublisher(eventPublisher);
            
            // 6. Health Check Manager
            logger.info("Initializing Health Check Manager...");
            this.healthCheckManager = new HealthCheckManager(
                containerManager,
                dockerExecutor,
                eventPublisher
            );

            // 7. MQTT Message Parser
            logger.info("Initializing MQTT Message Parser...");
            this.mqttMessageParser = new MqttMessageParser();
            
            // 8. Thread Pools for Async Operations
            this.healthCheckScheduler = Executors.newScheduledThreadPool(2);
            this.taskExecutor = Executors.newFixedThreadPool(4);
            
            logger.info("All managers initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize managers", e);
            throw new RuntimeException("HostManager initialization failed", e);
        }
    }
    
    /**
     * Verify Docker installation and accessibility
     */
    private void verifyDockerInstallation() {
        String dockerVersion = dockerExecutor.getDockerVersion();
        if (dockerVersion == null || dockerVersion.contains("Unknown")) {
            logger.warn("Docker verification warning: {}", dockerVersion);
        } else {
            logger.info("Docker verified: {}", dockerVersion);
        }
    }
    
    /**
     * Start the HostManager application
     */
    public synchronized void start() {
        if (isRunning) {
            logger.warn("HostManager already running");
            return;
        }
        
        try {
            logger.info("========== Starting HostManager ==========");
            
            // Synchronize with existing containers
            synchronizeContainerState();

            // Subscribe to MQTT topics for scaling requests
            subscribeToScalingRequests();
            
            // Start periodic health checks
            startHealthCheckScheduler();
            
            // Mark as running
            isRunning = true;
            
            logger.info("========== HostManager Started Successfully ==========");
            logger.info("Container Manager Status: {}", containerManager.getSummary());
            logger.info("Health Check Scheduler: ACTIVE");
            logger.info("MQTT Broker: CONNECTED");
            
        } catch (Exception e) {
            logger.error("Failed to start HostManager", e);
            stop();
            throw new RuntimeException("HostManager startup failed", e);
        }
    }

    /**
     * Synchronize with existing containers on startup
     */
    private void synchronizeContainerState() {
        logger.info("Synchronizing container state...");
        ProcessResult result = dockerExecutor.listContainersByName("soft40051-files-container");
        if (result.getExitCode() == 0) {
            String[] containerNames = result.getOutput().split("\n");
            for (String name : containerNames) {
                if (!name.trim().isEmpty()) {
                    containerManager.addContainer(name.trim());
                    logger.info("Found existing container: {}", name.trim());
                }
            }
        } else {
            logger.error("Failed to list existing containers: {}", result.getError());
        }
    }
    
    /**
     * Subscribe to MQTT topics for scaling requests from Load Balancer
     */
    private void subscribeToScalingRequests() {
        try {
            // Topic: loadbalancer/scaling/requests
            // Message format: {"action": "SCALE_UP|SCALE_DOWN", "quantity": N}
            mqttConnectionManager.subscribe(MqttConstants.TOPIC_SCALING_REQUESTS, (topic, message) -> {
                taskExecutor.submit(() -> handleScalingRequest(new String(message.getPayload())));
            });
            
            logger.info("Subscribed to MQTT topic: {}", MqttConstants.TOPIC_SCALING_REQUESTS);
            
        } catch (Exception e) {
            logger.error("Failed to subscribe to scaling requests", e);
        }
    }
    
    /**
     * Handle incoming scaling requests from Load Balancer
     * 
     * @param message MQTT message containing scaling request
     */
    private void handleScalingRequest(String message) {
        try {
            logger.info("Received scaling request: {}", message);
            
            ScalingRequest request = mqttMessageParser.parse(message);
            
            if ("up".equalsIgnoreCase(request.getAction())) {
                scalingLogic.handleScaleUp(request.getCount());
            } else if ("down".equalsIgnoreCase(request.getAction())) {
                scalingLogic.handleScaleDown(request.getCount());
            } else {
                logger.warn("Unknown scaling action: {}", request.getAction());
            }
            
        } catch (Exception e) {
            logger.error("Error handling scaling request", e);
        }
    }
    
    /**
     * Start periodic health checks for all containers
     */
    private void startHealthCheckScheduler() {
        // Run health checks every 10 seconds
        healthCheckScheduler.scheduleAtFixedRate(() -> {
            try {
                healthCheckManager.performHealthCheck();
            } catch (Exception e) {
                logger.error("Health check execution failed", e);
            }
        }, 5, 10, TimeUnit.SECONDS);
        
        logger.info("Health check scheduler started (interval: 10 seconds)");
    }
    
    /**
     * Stop the HostManager application - Graceful shutdown
     */
    public synchronized void stop() {
        if (!isRunning) {
            logger.warn("HostManager not running");
            return;
        }
        
        try {
            logger.info("========== Stopping HostManager ==========");
            
            isRunning = false;
            
            // 1. Shutdown health check scheduler
            if (healthCheckScheduler != null) {
                healthCheckScheduler.shutdown();
                if (!healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckScheduler.shutdownNow();
                }
                logger.info("Health check scheduler stopped");
            }
            
            // 2. Shutdown task executor
            if (taskExecutor != null) {
                taskExecutor.shutdown();
                if (!taskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    taskExecutor.shutdownNow();
                }
                logger.info("Task executor stopped");
            }
            
            // 3. Stop all containers gracefully
            stopAllContainers();
            
            // 4. Disconnect MQTT
            if (mqttConnectionManager != null) {
                mqttConnectionManager.disconnect();
                logger.info("MQTT disconnected");
            }
            
            logger.info("========== HostManager Stopped Successfully ==========");
            
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
    
    /**
     * Stop all running containers gracefully
     */
    private void stopAllContainers() {
        try {
            List<String> runningContainers = new ArrayList<>();
            containerManager.getAllContainers().forEach(c -> {
                if (c.isHealthy()) {
                    runningContainers.add(c.getContainerName());
                }
            });
            
            if (!runningContainers.isEmpty()) {
                logger.info("Stopping {} containers...", runningContainers.size());
                for (String containerName : runningContainers) {
                    dockerExecutor.stopContainer(containerName);
                    containerManager.removeContainer(containerName);
                    logger.info("Stopped container: {}", containerName);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error stopping containers", e);
        }
    }
    
    /**
     * Scale up - Add new containers
     * 
     * @param quantity Number of containers to add
     */
    public void scaleUp(int quantity) {
        if (!isRunning) {
            logger.warn("Cannot scale: HostManager not running");
            return;
        }
        taskExecutor.submit(() -> scalingLogic.handleScaleUp(quantity));
    }
    
    /**
     * Scale down - Remove containers
     * 
     * @param quantity Number of containers to remove
     */
    public void scaleDown(int quantity) {
        if (!isRunning) {
            logger.warn("Cannot scale: HostManager not running");
            return;
        }
        taskExecutor.submit(() -> scalingLogic.handleScaleDown(quantity));
    }
    
    /**
     * Get current system status
     */
    public String getStatus() {
        return String.format(
            "HostManager Status [running=%s, containers=%s, health=%s]",
            isRunning,
            containerManager.getSummary(),
            healthCheckManager.getHealthStatus()
        );
    }
    
    /**
     * Get container manager
     */
    public ContainerManager getContainerManager() {
        return containerManager;
    }
    
    /**
     * Get Docker executor
     */
    public DockerCommandExecutor getDockerExecutor() {
        return dockerExecutor;
    }
    
    /**
     * Get MQTT connection manager
     */
    public MqttConnectionManager getMqttConnectionManager() {
        return mqttConnectionManager;
    }
    
    /**
     * Main entry point - Start HostManager
     */
    public static void main(String[] args) {
        try {
            // Create and start HostManager
            HostManager hostManager = new HostManager();
            hostManager.start();
            
            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received");
                hostManager.stop();
            }));
            
            // Keep application running
            logger.info("HostManager is running. Press Ctrl+C to stop.");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Fatal error: HostManager startup failed", e);
            System.exit(1);
        }
    }
}
