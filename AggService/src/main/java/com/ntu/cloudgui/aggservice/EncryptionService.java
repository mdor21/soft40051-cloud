package com.ntu.cloudgui.aggservice;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES";
    private final SecretKey secretKey;

    public EncryptionService(String key) {
        this.secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
    }

    public byte[] encrypt(byte[] data) throws ProcessingException {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            logger.error("Error encrypting data", e);
            throw new ProcessingException("Error during data encryption", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) throws ProcessingException {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            logger.error("Error decrypting data", e);
            throw new ProcessingException("Error during data decryption", e);
        }
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(256, new SecureRandom());
        return keyGen.generateKey();
    }
}
