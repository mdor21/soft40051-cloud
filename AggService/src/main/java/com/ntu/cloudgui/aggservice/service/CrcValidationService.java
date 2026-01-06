package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;

import java.util.zip.CRC32;

public class CrcValidationService {

    public CrcValidationService() {
    }

    public long calculateCrc32(byte[] data) throws ProcessingException {
        try {
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            return crc32.getValue();
        } catch (Exception e) {
            throw new ProcessingException("CRC32 calculation failed: " + e.getMessage(), ErrorType.VALIDATION_ERROR, e);
        }
    }

    public void validateCrc32(long calculatedCrc, long expectedCrc) throws ProcessingException {
        if (calculatedCrc != expectedCrc) {
            throw new ProcessingException(String.format("Data corruption detected: CRC32 mismatch (got %d, expected %d)", calculatedCrc, expectedCrc), ErrorType.VALIDATION_ERROR);
        }
    }

    public String getCrcAsHex(long crc) {
        return "0x" + Long.toHexString(crc).toUpperCase();
    }
}
