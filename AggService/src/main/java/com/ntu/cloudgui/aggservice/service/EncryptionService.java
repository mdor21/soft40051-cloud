package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * EncryptionService - AES Encryption and Decryption
 * 
 * Provides symmetric encryption using AES in GCM mode.
 * 
 * Features:
 * - AES/256/GCM encryption (authenticated)
 * - Per-chunk random IV (initialization vector)
 * - Automatic key generation
 * - Base64 encoding for key storage
 * - Authenticated encryption with associated data (AEAD)
 * 
 * Security Properties:
 * ✓ 256-bit keys (AES-256)
 * ✓ Galois/Counter Mode (GCM) for authentication
 * ✓ Random IV per chunk (prevents pattern attacks)
 * ✓ 128-bit authentication tag
 * ✓ Uses SecureRandom for randomness
 * 
 * GCM Mode Benefits:
 * - Provides both confidentiality and authenticity
 * - Detects tampering automatically
 * - No separate MAC needed
 * - IV can be public (randomness is key)
 * 
 * Example:
 * <pre>
 * EncryptionService encService = new EncryptionService();
 * 
 * // Generate key once, store securely
 * SecretKey key = encService.generateKey();
 * String keyString = encService.encodeKey(key);
 * 
 * // Encrypt data
 * byte[] plaintext = "Sensitive data".getBytes();
 * byte[] ciphertext = encService.encrypt(plaintext, key);
 * 
 * // Decrypt data
 * byte[] decrypted = encService.decrypt(ciphertext, key);
 * </pre>
 */
public class EncryptionService {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    
    // Encryption constants
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;  // AES-256
    private static final int IV_SIZE = 12;  // 96 bits for GCM (standard)
    private static final int TAG_SIZE = 128;  // 128-bit authentication tag
    
    private final SecureRandom secureRandom;
    
    /**
     * Constructor - Initialize encryption service
     */
    public EncryptionService() {
        this.secureRandom = new SecureRandom();
        logger.info("EncryptionService initialized: AES-256/GCM/NoPadding");
    }
    
    /**
     * Generate random encryption key
     * 
     * Creates a new 256-bit AES key using SecureRandom.
     * Should be generated once per system and stored securely.
     * 
     * @return SecretKey (AES-256)
     * @throws ProcessingException if key generation fails
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
                ProcessingException.ErrorType.ENCRYPTION_ERROR,
                "Failed to generate encryption key: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Encrypt plaintext
     * 
     * Encrypts data using AES-256/GCM with random IV.
     * Each encryption uses a fresh random IV.
     * 
     * IV is prepended to ciphertext: [IV (12 bytes)][CIPHERTEXT][TAG]
     * This allows decryption without storing IV separately.
     * 
     * @param plaintext Data to encrypt
     * @param keyString Base64-encoded key (from encodeKey())
     * @return Encrypted data with IV prepended
     * @throws ProcessingException if encryption fails
     */
    public byte[] encrypt(byte[] plaintext, String keyString) 
            throws ProcessingException {
        
        logger.debug("Encrypting data ({} bytes)", plaintext.length);
        
        try {
            // Decode key from base64
            SecretKey key = decodeKey(keyString);
            
            // Generate random IV
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            
            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            
            // Encrypt data
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Combine IV + ciphertext for transmission
            byte[] encryptedData = new byte[IV_SIZE + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, IV_SIZE);
            System.arraycopy(ciphertext, 0, encryptedData, IV_SIZE, ciphertext.length);
            
            logger.debug("✓ Data encrypted: {} → {} bytes", 
                        plaintext.length, encryptedData.length);
            
            return encryptedData;
            
        } catch (Exception e) {
            logger.error("✗ Encryption failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                ProcessingException.ErrorType.ENCRYPTION_ERROR,
                "Encryption failed: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Decrypt ciphertext
     * 
     * Decrypts data that was encrypted with encrypt().
     * Extracts IV from beginning of ciphertext.
     * Automatically verifies authentication tag (GCM mode).
     * 
     * If IV extraction or tag verification fails, decryption fails.
     * This ensures data integrity.
     * 
     * @param encryptedData Encrypted data (with IV prepended)
     * @param keyString Base64-encoded key (from encodeKey())
     * @return Decrypted plaintext
     * @throws ProcessingException if decryption or verification fails
     */
    public byte[] decrypt(byte[] encryptedData, String keyString) 
            throws ProcessingException {
        
        logger.debug("Decrypting data ({} bytes)", encryptedData.length);
        
        try {
            // Validate minimum size (IV + at least 1 byte ciphertext + tag)
            if (encryptedData.length < IV_SIZE + 16) {
                throw new ProcessingException(
                    ProcessingException.ErrorType.ENCRYPTION_ERROR,
                    "Invalid encrypted data: too short"
                );
            }
            
            // Decode key from base64
            SecretKey key = decodeKey(keyString);
            
            // Extract IV from beginning
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);
            
            // Extract ciphertext
            byte[] ciphertext = new byte[encryptedData.length - IV_SIZE];
            System.arraycopy(encryptedData, IV_SIZE, ciphertext, 0, ciphertext.length);
            
            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            
            // Decrypt data (automatically verifies authentication tag)
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            logger.debug("✓ Data decrypted: {} → {} bytes", 
                        encryptedData.length, plaintext.length);
            
            return plaintext;
            
        } catch (javax.crypto.AEADBadTagException e) {
            logger.error("✗ Authentication tag verification failed (data corrupted): {}", 
                        e.getMessage());
            throw new ProcessingException(
                ProcessingException.ErrorType.ENCRYPTION_ERROR,
                "Data integrity check failed - data may be corrupted",
                e
            );
            
        } catch (Exception e) {
            logger.error("✗ Decryption failed: {}", e.getMessage(), e);
            throw new ProcessingException(
                ProcessingException.ErrorType.ENCRYPTION_ERROR,
                "Decryption failed: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Encode key to Base64 string
     * 
     * Converts SecretKey to Base64-encoded string for storage.
     * Can be stored in configuration, environment variables, or vault.
     * 
     * @param key SecretKey to encode
     * @return Base64-encoded key string
     */
    public String encodeKey(SecretKey key) {
        String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        logger.debug("Key encoded to base64 ({} chars)", encoded.length());
        return encoded;
    }
    
    /**
     * Decode key from Base64 string
     * 
     * Converts Base64-encoded key string back to SecretKey.
     * Inverse of encodeKey().
     * 
     * @param keyString Base64-encoded key string
     * @return Decoded SecretKey
     * @throws ProcessingException if decoding fails
     */
    private SecretKey decodeKey(String keyString) throws ProcessingException {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(keyString);
            
            // Validate key size
            if (decodedKey.length != KEY_SIZE / 8) {
                throw new ProcessingException(
                    ProcessingException.ErrorType.ENCRYPTION_ERROR,
                    String.format("Invalid key size: expected %d bytes, got %d", 
                                 KEY_SIZE / 8, decodedKey.length)
                );
            }
            
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            
        } catch (IllegalArgumentException e) {
            throw new ProcessingException(
                ProcessingException.ErrorType.ENCRYPTION_ERROR,
                "Invalid base64 key format: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Get encryption algorithm info
     * 
     * @return Algorithm string (AES/GCM/NoPadding)
     */
    public String getAlgorithm() {
        return ALGORITHM;
    }
}
