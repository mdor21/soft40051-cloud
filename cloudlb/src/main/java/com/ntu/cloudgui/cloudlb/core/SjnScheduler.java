package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SjnScheduler - Shortest Job Next Scheduler
 *
 * This scheduler works in conjunction with the RequestQueue, which is responsible
 * for prioritizing requests based on job size (smaller jobs first) and age.
 *
 * This scheduler's role is simply to select the next available healthy node
 * in a round-robin fashion. The "Shortest-Job-Next" logic is enforced by the
 * queue, which provides the highest-priority (i.e., smallest) job to the worker.
 *
 * Thread-safe: uses AtomicInteger for safe round-robin index updates.
 */
public class SjnScheduler implements Scheduler {

    private static final String LOG_PREFIX = "[SJN]";
    private final AtomicInteger currentIndex = new AtomicInteger(0);

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
     * Select a node using a round-robin strategy.
     *
     * The RequestQueue has already provided the "shortest job," so this scheduler's
     * task is to distribute these high-priority jobs across the available nodes.
     *
     * @param healthyNodes List of healthy storage nodes
     * @param request The request to be scheduled (already prioritized by the queue)
     * @return Selected StorageNode, or null if no healthy nodes are available
     */
    @Override
    public StorageNode selectNode(List<StorageNode> healthyNodes, Request request) {
        if (healthyNodes == null || healthyNodes.isEmpty()) {
            System.out.printf("%s No healthy nodes available%n", LOG_PREFIX);
            return null;
        }

        // Use thread-safe round-robin to select the next healthy node.
        int index = Math.abs(currentIndex.getAndIncrement()) % healthyNodes.size();
        StorageNode selectedNode = healthyNodes.get(index);

        System.out.printf("%s Selected node %s for shortest job %s (%.2f MB)%n",
            LOG_PREFIX,
            selectedNode.getName(),
            request.getId(),
            request.getSizeMB());

        return selectedNode;
    }
}
