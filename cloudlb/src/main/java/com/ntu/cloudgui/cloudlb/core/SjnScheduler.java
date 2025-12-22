package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import java.util.List;

/**
 * SjnScheduler - Shortest Job Next Scheduler
 * 
 * Selects storage nodes based on job size/load.
 * 
 * Current implementation: Simple round-robin selection from healthy nodes.
 * (In production, this would track per-node load and select least-loaded node)
 * 
 * Thread-safe: synchronized selectNode() method for safe index updates.
 */
public class SjnScheduler implements Scheduler {

    private static final String LOG_PREFIX = "[SJN]";
    private int currentIndex = 0;  // For round-robin behavior

    /**
     * Get scheduler name.
     * 
     * @return Scheduler name
     */
    @Override
    public String getName() {
        return "SJN";
    }

    /**
     * Select a node using SJN strategy.
     * 
     * Current: Round-robin selection (picks nodes in sequence)
     * Future: Could track node load and select least-loaded
     * 
     * @param healthyNodes List of healthy storage nodes
     * @param request Request to be scheduled
     * @return Selected StorageNode, or null if no healthy nodes
     */
    @Override
    public synchronized StorageNode selectNode(List<StorageNode> healthyNodes, Request request) {
        if (healthyNodes == null || healthyNodes.isEmpty()) {
            System.out.printf("%s No healthy nodes available%n", LOG_PREFIX);
            return null;
        }

        // Simple round-robin selection
        // (Replace with load-based selection when load tracking is implemented)
        StorageNode selectedNode = healthyNodes.get(currentIndex % healthyNodes.size());
        currentIndex = (currentIndex + 1) % healthyNodes.size();
        
        System.out.printf("%s Selected: %s (%s)%n",
            LOG_PREFIX,
            selectedNode.getName(),
            selectedNode.getAddress());
        
        return selectedNode;
    }
}
