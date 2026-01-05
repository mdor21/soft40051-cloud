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
    private static final String MQTT_BROKER_URL = "tcp://mqtt-broker:1883";
    private static final String MQTT_CLIENT_ID = "cloudlb";
    private static final String MQTT_TOPIC = "lb/scale/request";

    // Node configuration
    private static final String[] NODE_NAMES = {
        "node-1", "node-2", "node-3", "node-4", "node-5"
    };

    private static final String[] NODE_ADDRESSES = {
        "aggservice-1:8080",
        "aggservice-2:8080",
        "aggservice-3:8080",
        "aggservice-4:8080",
        "aggservice-5:8080"
    };

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
            int nodeCount = getNodeCount();
            String schedulerType = getSchedulerType();

            // Create core components
            System.out.println("[Main] Initializing core components...");
            RequestQueue requestQueue = new RequestQueue();
            NodeRegistry nodeRegistry = new NodeRegistry();
            Scheduler scheduler = createScheduler(schedulerType);

            // Register storage nodes
            System.out.printf("[Main] Registering %d storage nodes:%n", nodeCount);
            registerStorageNodes(nodeRegistry, nodeCount);

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
            int scalingUpThreshold = 80; // Example value
            int scalingDownThreshold = 20; // Example value
            ScalingService scalingService = new ScalingService(requestQueue, MQTT_BROKER_URL, MQTT_CLIENT_ID, MQTT_TOPIC, scalingUpThreshold, scalingDownThreshold);
            ScheduledExecutorService scalingScheduler = Executors.newSingleThreadScheduledExecutor();
            scalingScheduler.scheduleAtFixedRate(scalingService::checkAndScale, 0, SCALING_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
            System.out.printf("[Main] ✓ Scaling service started (interval: %d ms)%n", SCALING_CHECK_INTERVAL_MS);


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
            System.out.printf("[Main] Storage Nodes: %d%n", nodeCount);
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
     * Get node count from environment variable.
     *
     * @return Number of nodes to register (1-5, default: 2)
     */
    private static int getNodeCount() {
        String envValue = System.getenv("NODE_COUNT");
        if (envValue != null) {
            try {
                int count = Integer.parseInt(envValue);
                if (count >= 1 && count <= NODE_NAMES.length) {
                    return count;
                }
                System.out.printf("[Main] Invalid NODE_COUNT: %d (using default: %d)%n",
                    count, DEFAULT_NODE_COUNT);
            } catch (NumberFormatException e) {
                System.out.printf("[Main] Invalid NODE_COUNT format: %s (using default: %d)%n",
                    envValue, DEFAULT_NODE_COUNT);
            }
        }
        return DEFAULT_NODE_COUNT;
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
     * Register storage nodes in the registry.
     *
     * @param nodeRegistry Registry to register nodes in
     * @param nodeCount Number of nodes to register
     */
    private static void registerStorageNodes(NodeRegistry nodeRegistry, int nodeCount) {
        for (int i = 0; i < nodeCount && i < NODE_NAMES.length; i++) {
            StorageNode node = new StorageNode(NODE_NAMES[i], NODE_ADDRESSES[i]);
            nodeRegistry.registerNode(node);
            System.out.printf("[Main]   • %s (%s)%n", NODE_NAMES[i], NODE_ADDRESSES[i]);
        }
    }
}
