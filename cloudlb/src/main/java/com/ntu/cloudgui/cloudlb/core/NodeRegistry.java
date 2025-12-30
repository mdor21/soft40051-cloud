package com.ntu.cloudgui.cloudlb.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * NodeRegistry Class - Thread-safe storage node management
 * 
 * Manages a registry of all available storage/aggregator nodes with:
 * - Thread-safe add/remove operations
 * - Health status tracking
 * - Filtering for healthy nodes
 * - Dynamic scaling support (add/remove nodes at runtime)
 * 
 * Features:
 * - ReentrantReadWriteLock for thread safety
 * - Multiple readers allowed (getHealthyNodes)
 * - Exclusive writer access (register/unregister)
 * - Fast health queries
 * 
 * Thread Safety: Thread-safe (ReentrantReadWriteLock)
 * 
 * Usage:
 * ```
 * NodeRegistry registry = new NodeRegistry();
 * registry.registerNode(new StorageNode("node-1", "aggservice-1:8080"));
 * registry.registerNode(new StorageNode("node-2", "aggservice-2:8080"));
 * 
 * List<StorageNode> healthy = registry.getHealthyNodes();
 * registry.markNodeUnhealthy("node-1");
 * ```
 */
public class NodeRegistry {

    private final List<StorageNode> nodes;
    private final ReentrantReadWriteLock lock;

    /**
     * Create a new node registry.
     * 
     * Initializes empty registry with read-write lock.
     */
    public NodeRegistry() {
        this.nodes = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Register a new storage node.
     * 
     * Thread-safe operation. Duplicate nodes are allowed
     * (same node can be registered multiple times).
     * 
     * @param node Node to register
     */
    public void registerNode(StorageNode node) {
        lock.writeLock().lock();
        try {
            nodes.add(node);
            System.out.printf("[NodeRegistry] Registered: %s (%s)%n",
                node.getName(),
                node.getAddress());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Unregister a storage node.
     * 
     * Removes first occurrence of node with given name.
     * Thread-safe operation.
     * 
     * @param nodeName Name of node to remove
     * @return true if node was removed, false if not found
     */
    public boolean unregisterNode(String nodeName) {
        lock.writeLock().lock();
        try {
            boolean removed = nodes.removeIf(node -> node.getName().equals(nodeName));
            if (removed) {
                System.out.printf("[NodeRegistry] Unregistered: %s%n", nodeName);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get all registered nodes.
     * 
     * Returns a copy of the node list to prevent external
     * modification of internal state.
     * 
     * Thread-safe read operation.
     * 
     * @return List of all registered nodes
     */
    public List<StorageNode> getAllNodes() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(nodes);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get only healthy nodes.
     * 
     * Filters and returns list of nodes marked as healthy.
     * Useful for load balancer to exclude failed nodes.
     * 
     * Thread-safe read operation.
     * 
     * @return List of healthy nodes
     */
    public List<StorageNode> getHealthyNodes() {
        lock.readLock().lock();
        try {
            List<StorageNode> healthy = new ArrayList<>();
            for (StorageNode node : nodes) {
                if (node.isHealthy()) {
                    healthy.add(node);
                }
            }
            return healthy;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get node by name.
     * 
     * Searches for node with given name.
     * Thread-safe read operation.
     * 
     * @param nodeName Name to search for
     * @return Node if found, null otherwise
     */
    public StorageNode getNode(String nodeName) {
        lock.readLock().lock();
        try {
            return nodes.stream()
                .filter(n -> n.getName().equals(nodeName))
                .findFirst()
                .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Mark node as healthy.
     * 
     * Updates health status of node with given name.
     * Thread-safe operation.
     * 
     * @param nodeName Name of node to mark healthy
     * @return true if node found and updated
     */
    public boolean markNodeHealthy(String nodeName) {
        lock.readLock().lock();
        try {
            StorageNode node = nodes.stream()
                .filter(n -> n.getName().equals(nodeName))
                .findFirst()
                .orElse(null);
            
            if (node != null) {
                node.markHealthy();
                System.out.printf("[NodeRegistry] Marked healthy: %s%n", nodeName);
                return true;
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Mark node as unhealthy.
     * 
     * Updates health status of node with given name.
     * Thread-safe operation.
     * 
     * @param nodeName Name of node to mark unhealthy
     * @return true if node found and updated
     */
    public boolean markNodeUnhealthy(String nodeName) {
        lock.readLock().lock();
        try {
            StorageNode node = nodes.stream()
                .filter(n -> n.getName().equals(nodeName))
                .findFirst()
                .orElse(null);
            
            if (node != null) {
                node.markUnhealthy();
                System.out.printf("[NodeRegistry] Marked unhealthy: %s%n", nodeName);
                return true;
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get count of all registered nodes.
     * 
     * Thread-safe read operation.
     * 
     * @return Total number of nodes
     */
    public int getTotalNodeCount() {
        lock.readLock().lock();
        try {
            return nodes.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get count of healthy nodes.
     * 
     * Thread-safe read operation.
     * 
     * @return Number of healthy nodes
     */
    public int getHealthyNodeCount() {
        lock.readLock().lock();
        try {
            return (int) nodes.stream()
                .filter(StorageNode::isHealthy)
                .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clear all registered nodes.
     * 
     * Thread-safe write operation.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            nodes.clear();
            System.out.println("[NodeRegistry] Cleared all nodes");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get string representation of registry.
     * 
     * @return Registry status string
     */
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            int healthy = getHealthyNodeCount();
            return String.format("NodeRegistry{total=%d, healthy=%d}",
                nodes.size(),
                healthy);
        } finally {
            lock.readLock().unlock();
        }
    }
}