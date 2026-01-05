package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FcfsScheduler - First Come First Serve Scheduler
 *
 * Distributes requests in a round-robin fashion. Since the RequestQueue
 * provides requests in a FCFS order (when jobs have similar sizes and age),
 * this scheduler's role is to distribute them sequentially across healthy nodes.
 *
 * Thread-safe: uses AtomicInteger for safe index updates.
 */
public class RoundRobinDistributionScheduler implements Scheduler {

    private static final String LOG_PREFIX = "[RR-DIST]";
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    /**
     * Get scheduler name.
     *
     * @return Scheduler name
     */
    @Override
    public String getName() {
        return "ROUND_ROBIN_DISTRIBUTION";
    }

    /**
     * Select a node using a round-robin strategy.
     *
     * @param healthyNodes List of healthy storage nodes
     * @param request      Request to be scheduled
     * @return Selected StorageNode, or null if no healthy nodes
     */
    @Override
    public StorageNode selectNode(List<StorageNode> healthyNodes, Request request) {
        if (healthyNodes == null || healthyNodes.isEmpty()) {
            System.out.printf("%s No healthy nodes available%n", LOG_PREFIX);
            return null;
        }

        // Use thread-safe round-robin to select the next healthy node
        int index = Math.abs(currentIndex.getAndIncrement()) % healthyNodes.size();
        StorageNode selectedNode = healthyNodes.get(index);

        System.out.printf("%s Selected: %s (%s)%n",
                LOG_PREFIX,
                selectedNode.getName(),
                selectedNode.getAddress());

        return selectedNode;
    }
}
