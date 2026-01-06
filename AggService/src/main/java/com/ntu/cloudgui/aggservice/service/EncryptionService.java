package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.AEADBadTagException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    private final SecureRandom secureRandom;

    public EncryptionService() {
        this.secureRandom = new SecureRandom();
    }

    public SecretKey generateKey() throws ProcessingException {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE, secureRandom);
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new ProcessingException("Failed to generate encryption key: " + e.getMessage(), ErrorType.ENCRYPTION_ERROR, e);
        }
    }

    public byte[] encrypt(byte[] plaintext, String keyString) throws ProcessingException {
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
            return encryptedData;
        } catch (Exception e) {
            throw new ProcessingException("Encryption failed: " + e.getMessage(), ErrorType.ENCRYPTION_ERROR, e);
        }
    }

    public byte[] decrypt(byte[] encryptedData, String keyString) throws ProcessingException {
        try {
            if (encryptedData.length < IV_SIZE + 16) {
                throw new ProcessingException("Invalid encrypted data: too short", ErrorType.ENCRYPTION_ERROR);
            }
            SecretKey key = decodeKey(keyString);
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);
            byte[] ciphertext = new byte[encryptedData.length - IV_SIZE];
            System.arraycopy(encryptedData, IV_SIZE, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            throw new ProcessingException("Data integrity check failed - data may be corrupted", ErrorType.ENCRYPTION_ERROR, e);
        } catch (Exception e) {
            throw new ProcessingException("Decryption failed: " + e.getMessage(), ErrorType.ENCRYPTION_ERROR, e);
        }
    }

    public String encodeKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    private SecretKey decodeKey(String keyString) throws ProcessingException {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(keyString);
            if (decodedKey.length != KEY_SIZE / 8) {
                throw new ProcessingException(String.format("Invalid key size: expected %d bytes, got %d", KEY_SIZE / 8, decodedKey.length), ErrorType.ENCRYPTION_ERROR);
            }
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } catch (IllegalArgumentException e) {
            throw new ProcessingException("Invalid base64 key format: " + e.getMessage(), ErrorType.ENCRYPTION_ERROR, e);
        }
    }

    public String getAlgorithm() {
        return ALGORITHM;
    }
}
