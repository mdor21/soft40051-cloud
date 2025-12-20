// com/ntu/cloudlb/cluster/NodeRegistry.java
package com.ntu.cloudgui.cloudlb.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NodeRegistry {

    private final List<StorageNode> nodes = new ArrayList<>();

    public synchronized void addNode(StorageNode node) {
        nodes.add(node);
    }

    public synchronized List<StorageNode> getHealthyNodes() {
        return nodes.stream()
                .filter(n -> n.getStatus() == StorageNode.Status.HEALTHY)
                .collect(Collectors.toList());
    }

    public synchronized List<StorageNode> getAllNodes() {
        return new ArrayList<>(nodes);
    }
}
