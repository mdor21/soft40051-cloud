// com/ntu/cloudlb/core/RoundRobinScheduler.java
package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import com.ntu.cloudgui.cloudlb.core.StorageNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinScheduler implements Scheduler {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public StorageNode selectNode(List<StorageNode> nodes, Request req) {
        if (nodes.isEmpty()) return null;
        int i = Math.floorMod(index.getAndIncrement(), nodes.size());
        return nodes.get(i);
    }

    @Override
    public String getName() {
        return "ROUND_ROBIN";
    }
}
