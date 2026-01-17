package com.ntu.cloudgui.cloudlb;

import org.junit.Assert;
import org.junit.Test;

public class RequestTest {

    @Test
    public void compareToPrefersSmallerSizeWhenAgeIsEqual() {
        long fixedTimeMs = 1_000_000L;
        Request small = new Request("small", Request.Type.UPLOAD, 1_000, 0, fixedTimeMs);
        Request large = new Request("large", Request.Type.UPLOAD, 10_000_000, 0, fixedTimeMs);

        Assert.assertTrue(small.compareTo(large) < 0);
        Assert.assertTrue(large.compareTo(small) > 0);
    }

    @Test
    public void uploadAndDownloadFlagsReflectType() {
        Request upload = new Request("u", Request.Type.UPLOAD, 500, 0);
        Request download = new Request("d", Request.Type.DOWNLOAD, 0, 0);

        Assert.assertTrue(upload.isUpload());
        Assert.assertFalse(upload.isDownload());
        Assert.assertTrue(download.isDownload());
        Assert.assertFalse(download.isUpload());
    }
}
