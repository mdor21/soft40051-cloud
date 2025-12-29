package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import java.util.List;

/**
 * FcfsScheduler - First Come First Serve Scheduler
 * 
 * Simple round-robin scheduler that selects nodes sequentially.
 * Always picks the first available healthy node from the list.
 * 
 * Thread-safe: operates on immutable list passed to selectNode().
 */
public class FcfsScheduler implements Scheduler {

    private static final String LOG_PREFIX = "[FCFS]";

    /**
     * Get scheduler name.
     * 
     * @return Scheduler name
     */
    @Override
    public String getName() {
        return "FCFS";
    }

    /**
     * Select a node using FCFS (first healthy node).
     * 
     * @param healthyNodes List of healthy storage nodes
     * @param request Request to be scheduled
     * @return Selected StorageNode, or null if no healthy nodes
     */
    @Override
    public StorageNode selectNode(List<StorageNode> healthyNodes, Request request) {
        if (healthyNodes == null || healthyNodes.isEmpty()) {
            System.out.printf("%s No healthy nodes available%n", LOG_PREFIX);
            return null;
        }

        // FCFS: simply select the first healthy node
        StorageNode selectedNode = healthyNodes.get(0);
        
        System.out.printf("%s Selected: %s (%s)%n",
            LOG_PREFIX,
            selectedNode.getName(),
            selectedNode.getAddress());
        
        return selectedNode;
    }
}
