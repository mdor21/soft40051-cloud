package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.zip.CRC32;

/**
 * CrcValidationService - Data Integrity Validation
 * 
 * Provides CRC32 checksum calculation and validation.
 * Used to detect accidental data corruption during transmission.
 * 
 * CRC32 Properties:
 * - 32-bit cyclic redundancy check
 * - Fast computation
 * - Good at detecting random bit errors
 * - NOT cryptographically secure (don't use for security)
 * - Stored with chunk metadata for validation during download
 * 
 * Workflow:
 * - Upload: Calculate CRC32 of plaintext → store with chunk
 * - Download: Calculate CRC32 of decrypted chunk → compare
 * 
 * Example:
 * <pre>
 * CrcValidationService crcService = new CrcValidationService();
 * 
 * // Upload: calculate checksum
 * long crc = crcService.calculateCrc32(data);
 * // Store in database
 * 
 * // Download: validate checksum
 * byte[] decrypted = decrypt(encrypted);
 * long calculatedCrc = crcService.calculateCrc32(decrypted);
 * crcService.validateCrc32(calculatedCrc, storedCrc);
 * </pre>
 */
public class CrcValidationService {
    private static final Logger logger = LoggerFactory.getLogger(CrcValidationService.class);
    
    /**
     * Constructor - Initialize CRC validation service
     */
    public CrcValidationService() {
        logger.info("CrcValidationService initialized");
    }
    
    /**
     * Calculate CRC32 checksum
     * 
     * Computes 32-bit CRC checksum for data integrity validation.
     * Used to detect accidental bit errors during transmission.
     * 
     * @param data Data to checksum
     * @return CRC32 checksum value
     * @throws ProcessingException if calculation fails
     */
    public long calculateCrc32(byte[] data) throws ProcessingException {
        logger.debug("Calculating CRC32 ({} bytes)", data.length);
        
        try {
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            long checksum = crc32.getValue();
            
            logger.debug("✓ CRC32 calculated: {} (0x{})", 
                        checksum, Long.toHexString(checksum));
            
            return checksum;
            
        } catch (Exception e) {
            logger.error("✗ CRC32 calculation failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                ProcessingException.ErrorType.VALIDATION_ERROR,
                "CRC32 calculation failed: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Validate CRC32 checksum
     * 
     * Compares calculated checksum with expected checksum.
     * Throws exception if mismatch (data corruption detected).
     * 
     * @param calculatedCrc CRC32 value calculated from data
     * @param expectedCrc CRC32 value from metadata
     * @throws ProcessingException if checksums don't match
     */
    public void validateCrc32(long calculatedCrc, long expectedCrc) 
            throws ProcessingException {
        
        logger.debug("Validating CRC32: {} vs {}", 
                    calculatedCrc, expectedCrc);
        
        if (calculatedCrc != expectedCrc) {
            logger.error("✗ CRC32 mismatch! Calculated: {}, Expected: {}", 
                        calculatedCrc, expectedCrc);
            
            throw new ProcessingException(
                ProcessingException.ErrorType.VALIDATION_ERROR,
                String.format("Data corruption detected: CRC32 mismatch (got %d, expected %d)", 
                             calculatedCrc, expectedCrc)
            );
        }
        
        logger.debug("✓ CRC32 validation passed");
    }
    
    /**
     * Get CRC32 as hex string
     * 
     * Converts CRC32 value to hex for logging/display.
     * 
     * @param crc CRC32 value
     * @return Hex string representation (0x12345678)
     */
    public String getCrcAsHex(long crc) {
        return "0x" + Long.toHexString(crc).toUpperCase();
    }
}
