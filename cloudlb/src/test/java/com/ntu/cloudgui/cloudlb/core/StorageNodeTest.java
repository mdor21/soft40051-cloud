package com.ntu.cloudgui.cloudlb.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageNodeTest {

    @Test
    void testStorageNodeCreation() {
        StorageNode node = new StorageNode("node1", "localhost:9000");
        
        assertEquals("node1", node.getName());
        assertEquals("localhost:9000", node.getAddress());
        assertTrue(node.isHealthy()); // Should be healthy by default
    }

    @Test
    void testMarkHealthy() {
        StorageNode node = new StorageNode("node1", "localhost:9000");
        node.markUnhealthy();
        assertFalse(node.isHealthy());
        
        node.markHealthy();
        assertTrue(node.isHealthy());
    }

    @Test
    void testMarkUnhealthy() {
        StorageNode node = new StorageNode("node1", "localhost:9000");
        assertTrue(node.isHealthy());
        
        node.markUnhealthy();
        assertFalse(node.isHealthy());
    }

    @Test
    void testSetHealthy() {
        StorageNode node = new StorageNode("node1", "localhost:9000");
        
        node.setHealthy(false);
        assertFalse(node.isHealthy());
        
        node.setHealthy(true);
        assertTrue(node.isHealthy());
    }

    @Test
    void testToString() {
        StorageNode node = new StorageNode("node1", "localhost:9000");
        String expected = "StorageNode{name='node1', address='localhost:9000', healthy=true}";
        
        assertEquals(expected, node.toString());
    }
}
