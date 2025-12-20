// src/main/java/com/ntu/cloudgui/cloudlb/core/FcfsScheduler.java
package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.cluster.StorageNode;

import java.util.Comparator;
import java.util.List;

public class FcfsScheduler implements Scheduler {

    @Override
    public StorageNode selectNode(List<StorageNode> nodes, Request req) {
        // FCFS: just pick the least-loaded node;
        // the queue order (RequestQueue) already handles who is next.
        return nodes.stream()
                .min(Comparator.comparingInt(StorageNode::getCurrentLoad))
                .orElse(null);
    }

    @Override
    public String getName() {
        return "FCFS";
    }
}
