package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.cluster.NodeRegistry;
import com.ntu.cloudgui.cloudlb.cluster.StorageNode;

import java.util.List;
import java.util.Random;

/**
 * Worker thread that:
 * - Takes requests from the RequestQueue
 * - Selects a healthy StorageNode via Scheduler
 * - Adds artificial latency (1–5 seconds)
 * - Forwards the request to the chosen node
 */
public class LoadBalancerWorker implements Runnable {

    private final RequestQueue requestQueue;
    private final NodeRegistry nodeRegistry;
    private final Scheduler scheduler;
    private final Random random = new Random();

    public LoadBalancerWorker(RequestQueue requestQueue,
                              NodeRegistry nodeRegistry,
                              Scheduler scheduler) {
        this.requestQueue = requestQueue;
        this.nodeRegistry = nodeRegistry;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Take next request from priority queue (blocking)
                Request req = requestQueue.take();

                List<StorageNode> healthy = nodeRegistry.getHealthyNodes();
                if (healthy.isEmpty()) {
                    System.err.println("No healthy nodes available for request " + req.getId());
                    // simple policy: drop / retry later; here we just skip
                    continue;
                }

                StorageNode node = scheduler.selectNode(healthy, req);
                if (node == null) {
                    System.err.println("Scheduler returned null node for request " + req.getId());
                    continue;
                }

                // Artificial latency: 1.0–5.0 seconds
                double delaySeconds = 1.0 + random.nextDouble() * 4.0;
                try {
                    Thread.sleep((long) (delaySeconds * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                node.incrementLoad();
                try {
                    forwardToNode(node, req);
                } finally {
                    node.decrementLoad();
                }

            } catch (InterruptedException e) {
                // Graceful shutdown
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Forward the request to the chosen storage node.
     * For this coursework, the storage node is an Aggregator instance
     * exposing an HTTP API.
     *
     * You can extend this later to actually call AggService HTTP endpoints.
     */
    private void forwardToNode(StorageNode node, Request req) {
        // TODO: implement HTTP client call to Aggregator (AggService) node.
        // Example: http://node.getHost():node.getPort()/internal/files/...
        System.out.printf(
                "Forwarding %s to node %s (%s:%d) using %s%n",
                req.getId(),
                node.getId(),
                node.getHost(),
                node.getPort(),
                scheduler.getName()
        );
    }
}
