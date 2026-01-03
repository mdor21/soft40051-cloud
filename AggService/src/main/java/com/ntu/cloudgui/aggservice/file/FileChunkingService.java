package com.ntu.cloudgui.aggservice.file;

import com.ntu.cloudgui.aggservice.crypto.AESEncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;

/**
 * File Chunking and CRC32 Verification Service
 * 
 * Responsibilities:
 * - Split files into encrypted chunks (default: 1MB per chunk)
 * - Encrypt chunks using AES-256-GCM
 * - Calculate CRC32 checksums for each chunk
 * - Distribute chunks across file servers (1-4)
 * - Verify chunk integrity on download
 * 
 * Chunk Distribution Strategy:
 * Round-robin across available file servers for load balancing
 * 
 * CONNECTIVITY:
 * Aggregator REST API
 *     ↓
 * FileChunkingService.chunkFile() ← THIS CLASS
 *     ↓ (1MB chunks)
 * AESEncryptionService.encrypt() (AES-256-GCM) ✅
 *     ↓ (encrypted chunks)
 * FileServerSSHManager.uploadChunk()
 *     ↓ (SFTP over SSH)
 * File Servers (/data/chunks/)
 */
@Slf4j
@Service
public class FileChunkingService {
    
    private static final int DEFAULT_CHUNK_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_CHUNKS = 100; // Max 100 chunks per file
    
    @Autowired
    private AESEncryptionService encryptionService;
    
    /**
     * Split file into encrypted chunks with CRC32 checksums
     * 
     * @param fileData Complete file data
     * @param filename Original filename
     * @param chunkSize Size of each chunk in bytes
     * @return List of chunks with encryption and checksums
     */
    public List<FileChunk> chunkFile(byte[] fileData, String filename, int chunkSize) {
        if (chunkSize <= 0) {
            chunkSize = DEFAULT_CHUNK_SIZE;
        }
        
        List<FileChunk> chunks = new ArrayList<>();
        int totalChunks = (int) Math.ceil((double) fileData.length / chunkSize);
        
        if (totalChunks > MAX_CHUNKS) {
            throw new IllegalArgumentException(
                String.format("File too large: %d chunks exceeds max %d", totalChunks, MAX_CHUNKS));
        }
        
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, fileData.length);
            byte[] plainChunkData = Arrays.copyOfRange(fileData, start, end);
            
            // Calculate CRC32 of plaintext (for integrity verification)
            String plaintextCRC32 = calculateCRC32(plainChunkData);
            
            // Encrypt the chunk
            byte[] encryptedChunkData = encryptionService.encrypt(plainChunkData);
            
            FileChunk chunk = new FileChunk();
            chunk.setChunkId(UUID.randomUUID().toString());
            chunk.setFilename(filename);
            chunk.setChunkIndex(i);
            chunk.setTotalChunks(totalChunks);
            chunk.setData(encryptedChunkData);  // Store encrypted data
            chunk.setSize(encryptedChunkData.length);
            chunk.setPlaintextSize(plainChunkData.length);
            chunk.setChecksum(plaintextCRC32);  // CRC32 of plaintext
            chunk.setEncrypted(true);  // ✅ Mark as encrypted
            chunk.setEncryptionAlgorithm("AES-256-GCM");
            
            chunks.add(chunk);
            log.info(
                "Created chunk {}/{} for file {} (plaintext: {}B, encrypted: {}B, CRC32: {})",
                i + 1, totalChunks, filename, plainChunkData.length, 
                encryptedChunkData.length, plaintextCRC32);
        }
        
        return chunks;
    }
    
    /**
     * Reassemble file from encrypted chunks
     * 
     * @param chunks List of encrypted file chunks
     * @return Complete decrypted file data
     * @throws IllegalArgumentException if chunks are missing or corrupted
     */
    public byte[] reassembleFile(List<FileChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("No chunks provided");
        }
        
        // Sort by index
        chunks.sort(Comparator.comparingInt(FileChunk::getChunkIndex));
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        for (FileChunk chunk : chunks) {
            try {
                // Decrypt the chunk
                byte[] decryptedData = encryptionService.decrypt(chunk.getData());
                
                // Verify CRC32 of decrypted data
                String recalculatedCRC = calculateCRC32(decryptedData);
                if (!recalculatedCRC.equals(chunk.getChecksum())) {
                    throw new IllegalArgumentException(
                        String.format("Chunk %d corrupted (expected %s, got %s)",
                            chunk.getChunkIndex(), chunk.getChecksum(), recalculatedCRC));
                }
                
                buffer.write(decryptedData, 0, decryptedData.length);
                log.info("Decrypted and verified chunk {}/{} (size: {}B, CRC32: {})",
                    chunk.getChunkIndex() + 1, chunk.getTotalChunks(), 
                    decryptedData.length, recalculatedCRC);
                
            } catch (Exception e) {
                log.error("Failed to decrypt chunk {}", chunk.getChunkIndex(), e);
                throw new RuntimeException(
                    "Failed to reassemble file at chunk " + chunk.getChunkIndex(), e);
            }
        }
        
        log.info("Reassembled file from {} encrypted chunks", chunks.size());
        return buffer.toByteArray();
    }
    
    /**
     * Calculate CRC32 checksum for data
     * 
     * @param data Input data
     * @return CRC32 checksum as hex string
     */
    public String calculateCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return String.format("%08X", crc32.getValue());
    }
    
    /**
     * Get recommended file server for chunk (round-robin)
     * 
     * @param chunkIndex Chunk index
     * @param fileServerCount Available file servers (1-4)
     * @return File server index (0-3)
     */
    public int getFileServerForChunk(int chunkIndex, int fileServerCount) {
        if (fileServerCount <= 0 || fileServerCount > 4) {
            throw new IllegalArgumentException("File server count must be 1-4");
        }
        return chunkIndex % fileServerCount;
    }
    
    /**
     * File Chunk DTO - Enhanced with encryption
     */
    public static class FileChunk {
        private String chunkId;
        private String filename;
        private int chunkIndex;
        private int totalChunks;
        private byte[] data;  // Encrypted data
        private int size;  // Size of encrypted data
        private int plaintextSize;  // Size of plaintext before encryption
        private String checksum;  // CRC32 of plaintext
        private boolean encrypted;
        private String encryptionAlgorithm;
        
        // ... getters and setters
        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public int getPlaintextSize() { return plaintextSize; }
        public void setPlaintextSize(int plaintextSize) { this.plaintextSize = plaintextSize; }
        
        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }
        
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        
        public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
        public void setEncryptionAlgorithm(String encryptionAlgorithm) { 
            this.encryptionAlgorithm = encryptionAlgorithm; 
        }
    }
}
