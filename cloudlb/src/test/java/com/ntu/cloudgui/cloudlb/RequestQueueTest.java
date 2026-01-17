package com.ntu.cloudgui.cloudlb;

import org.junit.Assert;
import org.junit.Test;

public class RequestQueueTest {

    @Test
    public void addAndClearUpdateQueueSize() {
        RequestQueue queue = new RequestQueue();

        queue.add(new Request("file-1", Request.Type.UPLOAD, 2_000_000, 0));
        queue.add(new Request("file-2", Request.Type.DOWNLOAD, 0, 0));

        Assert.assertEquals(2, queue.size());

        queue.clear();
        Assert.assertTrue(queue.isEmpty());
    }

    @Test
    public void toStringIncludesQueueSize() {
        RequestQueue queue = new RequestQueue();
        queue.add(new Request("file-1", Request.Type.UPLOAD, 1_000, 0));

        String description = queue.toString();
        Assert.assertTrue(description.contains("size=1"));
        Assert.assertTrue(description.contains("Upload"));
    }
}
