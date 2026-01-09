package com.ntu.cloudgui.cloudlb.core;

import com.ntu.cloudgui.cloudlb.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinSchedulerTest {

    private RoundRobinScheduler scheduler;
    private List<StorageNode> nodes;

    @BeforeEach
    void setUp() {
        scheduler = new RoundRobinScheduler();
        nodes = new ArrayList<>();
        nodes.add(new StorageNode("node1", "localhost:9001"));
        nodes.add(new StorageNode("node2", "localhost:9002"));
        nodes.add(new StorageNode("node3", "localhost:9003"));
    }

    @Test
    void testGetName() {
        assertEquals("ROUND_ROBIN", scheduler.getName());
    }

    @Test
    void testSelectNode_RoundRobinDistribution() {
        Request req1 = new Request("file1", Request.Type.UPLOAD, 1000, 0);
        Request req2 = new Request("file2", Request.Type.UPLOAD, 2000, 0);
        Request req3 = new Request("file3", Request.Type.UPLOAD, 3000, 0);
        Request req4 = new Request("file4", Request.Type.UPLOAD, 4000, 0);

        StorageNode node1 = scheduler.selectNode(nodes, req1);
        StorageNode node2 = scheduler.selectNode(nodes, req2);
        StorageNode node3 = scheduler.selectNode(nodes, req3);
        StorageNode node4 = scheduler.selectNode(nodes, req4);

        assertEquals("node1", node1.getName());
        assertEquals("node2", node2.getName());
        assertEquals("node3", node3.getName());
        assertEquals("node1", node4.getName()); // Wraps around
    }

    @Test
    void testSelectNode_EmptyNodeList() {
        List<StorageNode> emptyNodes = new ArrayList<>();
        Request req = new Request("file1", Request.Type.UPLOAD, 1000, 0);
        
        StorageNode selectedNode = scheduler.selectNode(emptyNodes, req);
        assertNull(selectedNode);
    }

    @Test
    void testSelectNode_SingleNode() {
        List<StorageNode> singleNode = new ArrayList<>();
        singleNode.add(new StorageNode("onlyNode", "localhost:9000"));
        
        Request req1 = new Request("file1", Request.Type.UPLOAD, 1000, 0);
        Request req2 = new Request("file2", Request.Type.UPLOAD, 2000, 0);

        StorageNode node1 = scheduler.selectNode(singleNode, req1);
        StorageNode node2 = scheduler.selectNode(singleNode, req2);

        assertEquals("onlyNode", node1.getName());
        assertEquals("onlyNode", node2.getName());
    }
}
