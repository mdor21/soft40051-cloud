package com.ntu.cloudgui.aggservice.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM Encryption Service
 * 
 * Encrypts file chunks before transmission to file servers.
 * 
 * Algorithm: AES-256 in GCM (Galois/Counter Mode)
 * - Provides authenticated encryption (detects tampering)
 * - 256-bit key (32 bytes)
 * - 96-bit IV (12 bytes)
 * - 128-bit authentication tag
 * 
 * Security Properties:
 * - Confidentiality: Encrypts chunk data
 * - Integrity: GCM mode detects any data modification
 * - Authentication
 */
@Slf4j
@Service
public class AESEncryptionService {
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 96;
    private static final int AES_KEY_SIZE = 256;

    public String encrypt(String plaintext, String key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(key), 0, 32, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[GCM_IV_LENGTH / 8];
        random.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public String decrypt(String ciphertext, String key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(key), 0, 32, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[GCM_IV_LENGTH / 8];
        buffer.get(iv);
        byte[] ciphertextBytes = new byte[buffer.remaining()];
        buffer.get(ciphertextBytes);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return new String(cipher.doFinal(ciphertextBytes));
    }
}
