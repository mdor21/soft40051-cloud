package com.ntu.cloudgui.aggservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CrcValidationServiceTest {

    private CrcValidationService crcValidationService;

    @BeforeEach
    void setUp() {
        crcValidationService = new CrcValidationService();
    }

    @Test
    void testCalculateAndValidateCrc32() {
        String testData = "This is a test string for CRC32.";
        byte[] bytes = testData.getBytes();

        long crc32Value = crcValidationService.calculateCrc32(bytes);

        assertTrue(crcValidationService.validateCrc32(bytes, crc32Value));
    }

    @Test
    void testValidationFailure() {
        String testData = "This is a test string for CRC32.";
        byte[] bytes = testData.getBytes();

        long crc32Value = crcValidationService.calculateCrc32(bytes);

        // Corrupt the data
        bytes[0] = (byte) (bytes[0] + 1);

        assertFalse(crcValidationService.validateCrc32(bytes, crc32Value));
    }
}
