package com.ntu.cloudgui.cloudlb.core;

// com/ntu/cloudlb/core/SjnScheduler.java Shortest‑Job‑Next (by sizeBytes)


import com.ntu.cloudgui.cloudlb.cluster.StorageNode;
import java.util.Comparator;
import java.util.List;

public class SjnScheduler implements Scheduler {

    @Override
    public StorageNode selectNode(List<StorageNode> nodes, Request req) {
        // For now, use same logic as FCFS; SJN is for queue ordering (see RequestQueue)
        return nodes.stream()
                .min(Comparator.comparingInt(StorageNode::getCurrentLoad))
                .orElse(null);
    }

    @Override
    public String getName() {
        return "SJN";
    }
}
