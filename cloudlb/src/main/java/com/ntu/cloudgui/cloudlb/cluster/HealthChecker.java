// src/main/java/com/ntu/cloudgui/cloudlb/cluster/HealthChecker.java
package com.ntu.cloudgui.cloudlb.cluster;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthChecker {

    private final NodeRegistry registry;
    private final ScheduledExecutorService scheduler;

    public HealthChecker(NodeRegistry registry, ScheduledExecutorService scheduler) {
        this.registry = registry;
        this.scheduler = scheduler;
    }

    public void start(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::checkAll, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    private void checkAll() {
        List<StorageNode> nodes = registry.getAllNodes();
        for (StorageNode node : nodes) {
            boolean healthy = ping(node);
            node.setStatus(healthy ? StorageNode.Status.HEALTHY : StorageNode.Status.UNHEALTHY);
        }
    }

    private boolean ping(StorageNode node) {
        try (Socket s = new Socket(node.getHost(), node.getPort())) {
            s.setSoTimeout(1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
