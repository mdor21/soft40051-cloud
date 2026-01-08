package com.ntu.cloudgui.cloudlb;

import com.ntu.cloudgui.cloudlb.core.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MainLb - Load Balancer Entry Point
 *
 * Initializes and starts the complete load balancing system:
 * - Creates RequestQueue (thread-safe request buffer)
 * - Initializes NodeRegistry (storage node management)
 * - Registers storage nodes from configuration
 * - Selects and initializes Scheduler (FCFS/SJN/RoundRobin)
 * - Starts HealthChecker (monitors node health every 5 seconds)
 * - Starts ScalingService (monitors queue and requests scaling)
 * - Starts LoadBalancerWorker (processes requests from queue)
 * - Starts HTTP API server
 *
 * Configuration via environment variables:
 * - NODE_COUNT: Number of storage nodes (default: 2)
 * - SCHEDULER_TYPE: Scheduling algorithm (FCFS/SJN/ROUNDROBIN, default: ROUNDROBIN)
 *
 */
public class MainLb {

    // Configuration constants
    private static final int DEFAULT_NODE_COUNT = 2;
    private static final String DEFAULT_SCHEDULER = "ROUNDROBIN";
    private static final int API_SERVER_PORT = 8080;
    private static final int HEALTH_CHECK_INTERVAL_MS = 5000;  // 5 seconds
    private static final int SCALING_CHECK_INTERVAL_MS = 10000; // 10 seconds
    private static final String MQTT_CLIENT_ID = "cloudlb";
    private static final String MQTT_TOPIC = "lb/scale/request";


    /**
     * Main entry point for Load Balancer.
     *
     * @param args Command line arguments (not used; config via env vars)
     */
    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println("[Main] Starting Load Balancer...");
            System.out.println("========================================");

            // Parse configuration
            String schedulerType = getSchedulerType();

            // Create core components
            System.out.println("[Main] Initializing core components...");
            RequestQueue requestQueue = new RequestQueue();
            NodeRegistry nodeRegistry = new NodeRegistry();
            Scheduler scheduler = createScheduler(schedulerType);

            // Register storage nodes from environment variable
            registerStorageNodes(nodeRegistry);

            // Start health checker
            System.out.println("[Main] Starting Health Checker...");
            HealthChecker healthChecker = new HealthChecker(nodeRegistry, HEALTH_CHECK_INTERVAL_MS);
            Thread healthCheckerThread = new Thread(healthChecker);
            healthCheckerThread.setName("HealthChecker");
            healthCheckerThread.setDaemon(true);
            healthCheckerThread.start();
            System.out.printf("[Main] ✓ Health checker started (interval: %d ms)%n",
                HEALTH_CHECK_INTERVAL_MS);

            // Start scaling service
            System.out.println("[Main] Starting Scaling Service...");
            String mqttHost = System.getenv().getOrDefault("MQTT_BROKER_HOST", "mqtt-broker");
            String mqttPort = System.getenv().getOrDefault("MQTT_BROKER_PORT", "1883");
            String mqttBrokerUrl = "tcp://" + mqttHost + ":" + mqttPort;

            int scalingUpThreshold = 80; // Scale up if queue > 80
            int scalingDownThreshold = 20; // Scale down if queue < 20
            ScalingService scalingService = new ScalingService(
                requestQueue,
                mqttBrokerUrl,
                MQTT_CLIENT_ID,
                MQTT_TOPIC,
                scalingUpThreshold,
                scalingDownThreshold);
            ScheduledExecutorService scalingScheduler = Executors.newSingleThreadScheduledExecutor();
            scalingScheduler.scheduleAtFixedRate(scalingService::checkAndScale, 0, SCALING_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
            System.out.printf("[Main] ✓ Scaling service scheduled (interval: %d ms)%n", SCALING_CHECK_INTERVAL_MS);


            // Start load balancer worker
            System.out.println("[Main] Starting Load Balancer Worker...");
            LoadBalancerWorker worker = new LoadBalancerWorker(
                requestQueue, nodeRegistry, scheduler);
            Thread workerThread = new Thread(worker);
            workerThread.setName("LoadBalancer-Worker");
            workerThread.setDaemon(true);
            workerThread.start();
            System.out.println("[Main] ✓ Load Balancer Worker started");

            // Start HTTP API server
            System.out.println("[Main] Starting HTTP API Server...");
            try {
                LoadBalancerAPIServer apiServer = new LoadBalancerAPIServer(
                    requestQueue, nodeRegistry, scheduler, API_SERVER_PORT);
                apiServer.start();  // Blocking call - starts HTTP server
                System.out.printf("[Main] ✓ HTTP API Server listening on port %d%n",
                    API_SERVER_PORT);
            } catch (Exception e) {
                System.err.printf("[Main] Warning: Could not start API server: %s%n",
                    e.getMessage());
                System.err.println("[Main] Continuing without API server...");
            }

            // Startup complete
            System.out.println("========================================");
            System.out.println("[Main] ✓ Load Balancer fully initialized!");
            System.out.printf("[Main] Scheduler: %s%n", schedulerType.toUpperCase());
            System.out.printf("[Main] Storage Nodes: %d%n", nodeRegistry.getAllNodes().size());
            System.out.printf("[Main] API Server Port: %d%n", API_SERVER_PORT);
            System.out.println("========================================");

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            System.err.println("[Main] Load Balancer interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.printf("[Main] Fatal Error: %s%n", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Get scheduler type from environment variable.
     *
     * @return Scheduler type (FCFS/SJN/ROUNDROBIN, default: ROUNDROBIN)
     */
    private static String getSchedulerType() {
        String envValue = System.getenv("SCHEDULER_TYPE");
        if (envValue != null) {
            String upper = envValue.toUpperCase();
            if (upper.equals("FCFS") || upper.equals("SJN") || upper.equals("ROUNDROBIN")) {
                return upper;
            }
            System.out.printf("[Main] Invalid SCHEDULER_TYPE: %s (using default: %s)%n",
                envValue, DEFAULT_SCHEDULER);
        }
        return DEFAULT_SCHEDULER;
    }

    /**
     * Create scheduler instance based on type.
     *
     * @param schedulerType Type of scheduler (FCFS/SJN/ROUNDROBIN)
     * @return Scheduler instance
     */
    private static Scheduler createScheduler(String schedulerType) {
        switch (schedulerType) {
            case "FCFS":
                System.out.println("[Main] Scheduler: FCFS (Round Robin Distribution)");
                return new RoundRobinDistributionScheduler();

            case "SJN":
                System.out.println("[Main] Scheduler: SJN (Shortest Job Next)");
                return new SjnScheduler();

            case "ROUNDROBIN":
                System.out.println("[Main] Scheduler: ROUNDROBIN (Cyclic Distribution)");
                return new RoundRobinScheduler();

            default:
                throw new IllegalArgumentException("Unknown scheduler type: " + schedulerType);
        }
    }

    /**
     * Register storage nodes in the registry from the STORAGE_NODES environment variable.
     * The variable should be a comma-separated list of addresses, e.g.,
     * "aggservice-1:8080,aggservice-2:8080"
     *
     * @param nodeRegistry Registry to register nodes in.
     */
    private static void registerStorageNodes(NodeRegistry nodeRegistry) {
        String nodesEnv = System.getenv("STORAGE_NODES");
        if (nodesEnv == null || nodesEnv.trim().isEmpty()) {
            // Fallback to a default if the environment variable is not set.
            nodesEnv = "aggservice-1:9000,aggservice-2:9000";
            System.out.printf("[Main] WARNING: STORAGE_NODES env var not set. Falling back to default: %s%n", nodesEnv);
        }

        String[] nodeAddresses = nodesEnv.split(",");
        System.out.printf("[Main] Registering %d storage nodes:%n", nodeAddresses.length);

        for (int i = 0; i < nodeAddresses.length; i++) {
            String address = nodeAddresses[i].trim();
            if (address.isEmpty()) continue;
            // The node name is derived from the address for simplicity.
            String name = "node-" + (i + 1);
            StorageNode node = new StorageNode(name, address);
            nodeRegistry.registerNode(node);
            System.out.printf("[Main]   • %s (%s)%n", name, address);
        }
    }
}
