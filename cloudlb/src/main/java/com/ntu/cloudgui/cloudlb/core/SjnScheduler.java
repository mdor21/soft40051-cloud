package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
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
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            System.out.printf("[%s] %s No healthy nodes available for request %s%n", 
                timestamp, LOG_PREFIX, request.getId());
            return null;
        }

        // Use thread-safe round-robin to select the next healthy node.
        int index = Math.abs(currentIndex.getAndIncrement()) % healthyNodes.size();
        StorageNode selectedNode = healthyNodes.get(index);

        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] %s Selected node %s for request %s (%.2f MB) | Operation: %s | Wait Time: %dms%n",
            timestamp,
            LOG_PREFIX,
            selectedNode.getName(),
            request.getId(),
            request.getSizeMB(),
            request.getOperation(),
            System.currentTimeMillis() - request.getCreationTime());

        return selectedNode;
    }
}
