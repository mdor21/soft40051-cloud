package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.zip.CRC32;

/**
 * CrcValidationService - Data Integrity Validation.
 *
 * Provides CRC32 checksum calculation and validation.
 */
public class CrcValidationService {

    private static final Logger logger = LoggerFactory.getLogger(CrcValidationService.class);

    public CrcValidationService() {
        logger.info("CrcValidationService initialized");
    }

    /**
     * Calculate CRC32 checksum for the given data.
     */
    public long calculateCrc32(byte[] data) throws ProcessingException {
        logger.debug("Calculating CRC32 ({} bytes)", data.length);
        try {
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            long checksum = crc32.getValue();
            logger.debug("✓ CRC32 calculated: {} (0x{})", checksum, Long.toHexString(checksum));
            return checksum;
        } catch (Exception e) {
            logger.error("✗ CRC32 calculation failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "CRC32 calculation failed: " + e.getMessage(),
                    ErrorType.VALIDATION_ERROR,
                    e
            );
        }
    }

    /**
     * Validate that calculated CRC matches expected CRC.
     */
    public void validateCrc32(long calculatedCrc, long expectedCrc) throws ProcessingException {
        logger.debug("Validating CRC32: {} vs {}", calculatedCrc, expectedCrc);

        if (calculatedCrc != expectedCrc) {
            logger.error("✗ CRC32 mismatch! Calculated: {}, Expected: {}", calculatedCrc, expectedCrc);
            throw new ProcessingException(
                    String.format(
                            "Data corruption detected: CRC32 mismatch (got %d, expected %d)",
                            calculatedCrc, expectedCrc
                    ),
                    ErrorType.VALIDATION_ERROR
            );
        }

        logger.debug("✓ CRC32 validation passed");
    }

    /**
     * Format CRC32 as hex string (e.g. 0x1234ABCD).
     */
    public String getCrcAsHex(long crc) {
        return "0x" + Long.toHexString(crc).toUpperCase();
    }
}
