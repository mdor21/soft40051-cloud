package com.ntu.cloudgui.cloudlb.core;

/**
 * StorageNode Class - Represents a single storage/aggregator node
 * 
 * Immutable data class that holds information about:
 * - Node name/identifier
 * - Network address (host:port)
 * - Health status (healthy/unhealthy)
 * 
 * Thread Safety: Immutable (all fields final)
 * 
 * Usage:
 * ```
 * StorageNode node = new StorageNode("node-1", "aggservice-1:8080");
 * node.markHealthy();
 * if (node.isHealthy()) {
 *     // Forward request
 * }
 * ```
 */
public class StorageNode {

    private final String name;
    private final String address;
    private volatile boolean healthy;

    /**
     * Create a new storage node.
     * 
     * @param name Node identifier (e.g., "node-1")
     * @param address Network address in format "host:port" (e.g., "aggservice-1:8080")
     */
    public StorageNode(String name, String address) {
        this.name = name;
        this.address = address;
        this.healthy = true;  // Assume healthy on creation
    }

    /**
     * Get node name.
     * 
     * @return Node identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Get node network address.
     * 
     * @return Address in format "host:port"
     */
    public String getAddress() {
        return address;
    }

    /**
     * Check if node is healthy.
     * 
     * @return true if node is healthy
     */
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * Mark node as healthy.
     */
    public void markHealthy() {
        this.healthy = true;
    }

    /**
     * Mark node as unhealthy.
     */
    public void markUnhealthy() {
        this.healthy = false;
    }

    /**
     * Set health status.
     * 
     * @param healthy true for healthy, false for unhealthy
     */
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    /**
     * Get string representation.
     * 
     * @return Node information string
     */
    @Override
    public String toString() {
        return String.format("StorageNode{name='%s', address='%s', healthy=%s}",
            name, address, healthy);
    }
}