package com.ntu.cloudgui.aggservice;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class CrcValidationService {

    public long calculateCrc32(byte[] data) {
        Checksum crc32 = new CRC32();
        crc32.update(data, 0, data.length);
        return crc32.getValue();
    }

    public boolean validateCrc32(byte[] data, long expectedCrc32) {
        long actualCrc32 = calculateCrc32(data);
        return actualCrc32 == expectedCrc32;
    }
}
