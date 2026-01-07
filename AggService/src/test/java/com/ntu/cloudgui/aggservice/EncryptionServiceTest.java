package com.ntu.cloudgui.aggservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Use a fixed, valid 16-byte key for consistent testing
        String key = "aVerySecretKey12";
        encryptionService = new EncryptionService(key);
    }

    @Test
    void testEncryptAndDecrypt() throws ProcessingException {
        String originalText = "This is a test message.";
        byte[] originalBytes = originalText.getBytes();

        byte[] encryptedBytes = encryptionService.encrypt(originalBytes);
        assertNotNull(encryptedBytes);
        assertNotEquals(0, encryptedBytes.length);
        assertNotEquals(originalBytes.length, encryptedBytes.length);

        byte[] decryptedBytes = encryptionService.decrypt(encryptedBytes);
        assertNotNull(decryptedBytes);
        assertArrayEquals(originalBytes, decryptedBytes);

        String decryptedText = new String(decryptedBytes);
        assertEquals(originalText, decryptedText);
    }

    @Test
    void testEncryptWithEmptyData() throws ProcessingException {
        byte[] originalBytes = new byte[0];
        byte[] encryptedBytes = encryptionService.encrypt(originalBytes);
        byte[] decryptedBytes = encryptionService.decrypt(encryptedBytes);
        assertArrayEquals(originalBytes, decryptedBytes);
    }
}
