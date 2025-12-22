package com.ntu.cloudgui.cloudlb.core;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * HealthChecker - periodically verifies the health of all registered storage nodes.
 *
 * Responsibilities:
 * - Runs in a background thread at a fixed interval.
 * - Pings each StorageNode via TCP to its host:port address.
 * - Marks nodes healthy/unhealthy based on connectivity.
 * - Logs when nodes fail or recover.
 */
public class HealthChecker implements Runnable {

    private static final String LOG_PREFIX = "[HealthChecker]";
    private static final int PING_TIMEOUT_MS = 3000;   // 3 seconds

    private final NodeRegistry nodeRegistry;
    private final long intervalMs;
    private volatile boolean running = true;

    /**
     * Create a new HealthChecker.
     *
     * @param nodeRegistry Registry of nodes to monitor (must not be null).
     * @param intervalMs   Interval between health checks in milliseconds (> 0).
     */
    public HealthChecker(NodeRegistry nodeRegistry, long intervalMs) {
        if (nodeRegistry == null) {
            throw new IllegalArgumentException("nodeRegistry must not be null");
        }
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("intervalMs must be > 0");
        }
        this.nodeRegistry = nodeRegistry;
        this.intervalMs = intervalMs;
    }

    @Override
    public void run() {
        System.out.printf("%s Started (interval: %d ms)%n", LOG_PREFIX, intervalMs);

        while (running) {
            try {
                checkAllNodes();
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                System.out.printf("%s Interrupted, stopping%n", LOG_PREFIX);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.printf("%s Unexpected error: %s%n",
                        LOG_PREFIX, e.getMessage());
            }
        }

        System.out.printf("%s Stopped%n", LOG_PREFIX);
    }

    /**
     * Stop the health checker loop.
     * Call this from outside to let the thread exit gracefully.
     */
    public void stop() {
        running = false;
    }

    /**
     * @return true if the checker is still running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Check health of all nodes currently registered.
     */
    private void checkAllNodes() {
        List<StorageNode> allNodes = nodeRegistry.getAllNodes();
        if (allNodes.isEmpty()) {
            System.out.printf("%s No nodes to check%n", LOG_PREFIX);
            return;
        }

        for (StorageNode node : allNodes) {
            checkSingleNode(node);
        }
    }

    /**
     * Check the health of a single node and update its status if it changes.
     */
    private void checkSingleNode(StorageNode node) {
        boolean reachable = ping(node);
        boolean wasHealthy = node.isHealthy();

        if (reachable && !wasHealthy) {
            node.markHealthy();
            System.out.printf("%s ✓ RECOVERED: %s (%s)%n",
                    LOG_PREFIX, node.getName(), node.getAddress());
        } else if (!reachable && wasHealthy) {
            node.markUnhealthy();
            System.out.printf("%s ✗ FAILED: %s (%s)%n",
                    LOG_PREFIX, node.getName(), node.getAddress());
        }
    }

    /**
     * Try to open a TCP connection to the node's host:port.
     *
     * @return true if connection succeeds within timeout, false otherwise.
     */
    private boolean ping(StorageNode node) {
        String address = node.getAddress();
        String[] parts = address.split(":");
        if (parts.length != 2) {
            System.out.printf("%s Invalid address '%s' for node %s%n",
                    LOG_PREFIX, address, node.getName());
            return false;
        }

        String host = parts[0].trim();
        int port;
        try {
            port = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            System.out.printf("%s Invalid port in address '%s' for node %s%n",
                    LOG_PREFIX, address, node.getName());
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), PING_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            // Connection failed: considered unhealthy
            return false;
        }
    }
}
