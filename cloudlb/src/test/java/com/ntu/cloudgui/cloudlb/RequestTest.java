package com.ntu.cloudgui.cloudlb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testRequestCreation() {
        Request request = new Request("file123", Request.Type.UPLOAD, 1048576, 0);
        
        assertEquals("file123", request.getId());
        assertEquals(Request.Type.UPLOAD, request.getType());
        assertEquals(1048576, request.getSizeBytes());
        assertEquals(0, request.getPriority());
        assertTrue(request.isUpload());
        assertFalse(request.isDownload());
    }

    @Test
    void testDownloadRequest() {
        Request request = new Request("file456", Request.Type.DOWNLOAD, 2097152, 1);
        
        assertFalse(request.isUpload());
        assertTrue(request.isDownload());
        assertEquals(Request.Type.DOWNLOAD, request.getType());
    }

    @Test
    void testGetSizeMB() {
        Request request = new Request("file789", Request.Type.UPLOAD, 5000000, 0);
        assertEquals(5.0, request.getSizeMB(), 0.01);
    }

    @Test
    void testGetAgeMs() throws InterruptedException {
        Request request = new Request("file123", Request.Type.UPLOAD, 1000, 0);
        Thread.sleep(10); // Wait a bit
        
        assertTrue(request.getAgeMs() >= 10);
    }

    @Test
    void testEqualsAndHashCode() {
        Request req1 = new Request("file123", Request.Type.UPLOAD, 1000, 0);
        Request req2 = new Request("file123", Request.Type.DOWNLOAD, 2000, 1);
        Request req3 = new Request("file456", Request.Type.UPLOAD, 1000, 0);
        
        assertEquals(req1, req2); // Same ID
        assertNotEquals(req1, req3); // Different ID
        assertEquals(req1.hashCode(), req2.hashCode());
    }

    @Test
    void testToString() {
        Request request = new Request("file123", Request.Type.UPLOAD, 1000000, 0);
        String str = request.toString();
        
        assertTrue(str.contains("file123"));
        assertTrue(str.contains("Upload"));
    }

    @Test
    void testCompareTo() {
        // Create requests with specific creation times for predictable comparison
        long baseTime = System.currentTimeMillis();
        Request smallRequest = new Request("small", Request.Type.UPLOAD, 1000000, 0, baseTime);
        Request largeRequest = new Request("large", Request.Type.UPLOAD, 5000000, 0, baseTime);
        
        // Small request should have higher priority (come before) than large request
        assertTrue(smallRequest.compareTo(largeRequest) < 0);
    }
}
