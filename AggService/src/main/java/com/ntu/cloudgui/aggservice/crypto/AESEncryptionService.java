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
