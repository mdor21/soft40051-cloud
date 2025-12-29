package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.AEADBadTagException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * EncryptionService - AES Encryption and Decryption.
 *
 * Provides symmetric encryption using AES in GCM mode.
 */
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;   // AES-256
    private static final int IV_SIZE = 12;     // 96 bits for GCM
    private static final int TAG_SIZE = 128;   // 128‑bit auth tag

    private final SecureRandom secureRandom;

    public EncryptionService() {
        this.secureRandom = new SecureRandom();
        logger.info("EncryptionService initialized: AES-256/GCM/NoPadding");
    }

    /**
     * Generate a random 256‑bit AES key.
     */
    public SecretKey generateKey() throws ProcessingException {
        logger.debug("Generating encryption key (AES-256)");
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE, secureRandom);
            SecretKey key = keyGen.generateKey();
            logger.info("✓ Encryption key generated");
            return key;
        } catch (Exception e) {
            logger.error("✗ Failed to generate encryption key: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "Failed to generate encryption key: " + e.getMessage(),
                    ErrorType.ENCRYPTION_ERROR,
                    e
            );
        }
    }

    /**
     * Encrypt data using AES‑GCM with a random IV.
     *
     * Layout: [IV (12 bytes)] [CIPHERTEXT+TAG]
     */
    public byte[] encrypt(byte[] plaintext, String keyString) throws ProcessingException {
        logger.debug("Encrypting data ({} bytes)", plaintext.length);
        try {
            SecretKey key = decodeKey(keyString);

            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] encryptedData = new byte[IV_SIZE + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, IV_SIZE);
            System.arraycopy(ciphertext, 0, encryptedData, IV_SIZE, ciphertext.length);

            logger.debug("✓ Data encrypted: {} → {} bytes", plaintext.length, encryptedData.length);
            return encryptedData;
        } catch (Exception e) {
            logger.error("✗ Encryption failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "Encryption failed: " + e.getMessage(),
                    ErrorType.ENCRYPTION_ERROR,
                    e
            );
        }
    }

    /**
     * Decrypt data produced by {@link #encrypt(byte[], String)}.
     */
    public byte[] decrypt(byte[] encryptedData, String keyString) throws ProcessingException {
        logger.debug("Decrypting data ({} bytes)", encryptedData.length);
        try {
            if (encryptedData.length < IV_SIZE + 16) {
                throw new ProcessingException(
                        "Invalid encrypted data: too short",
                        ErrorType.ENCRYPTION_ERROR
                );
            }

            SecretKey key = decodeKey(keyString);

            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);

            byte[] ciphertext = new byte[encryptedData.length - IV_SIZE];
            System.arraycopy(encryptedData, IV_SIZE, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            logger.debug("✓ Data decrypted: {} → {} bytes", encryptedData.length, plaintext.length);
            return plaintext;
        } catch (AEADBadTagException e) {
            logger.error("✗ Authentication tag verification failed (data corrupted): {}", e.getMessage());
            throw new ProcessingException(
                    "Data integrity check failed - data may be corrupted",
                    ErrorType.ENCRYPTION_ERROR,
                    e
            );
        } catch (Exception e) {
            logger.error("✗ Decryption failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "Decryption failed: " + e.getMessage(),
                    ErrorType.ENCRYPTION_ERROR,
                    e
            );
        }
    }

    /**
     * Encode SecretKey to Base64.
     */
    public String encodeKey(SecretKey key) {
        String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        logger.debug("Key encoded to base64 ({} chars)", encoded.length());
        return encoded;
    }

    /**
     * Decode Base64 key string back to SecretKey.
     */
    private SecretKey decodeKey(String keyString) throws ProcessingException {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(keyString);

            if (decodedKey.length != KEY_SIZE / 8) {
                throw new ProcessingException(
                        String.format(
                                "Invalid key size: expected %d bytes, got %d",
                                KEY_SIZE / 8, decodedKey.length
                        ),
                        ErrorType.ENCRYPTION_ERROR
                );
            }

            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } catch (IllegalArgumentException e) {
            throw new ProcessingException(
                    "Invalid base64 key format: " + e.getMessage(),
                    ErrorType.ENCRYPTION_ERROR,
                    e
            );
        }
    }

    public String getAlgorithm() {
        return ALGORITHM;
    }
}
