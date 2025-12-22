package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import com.ntu.cloudgui.aggservice.exception.DatabaseException;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.repository.FileMetadataRepository;
import com.ntu.cloudgui.aggservice.repository.ChunkMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FileProcessingService - File Upload and Chunking Orchestration
 * 
 * Orchestrates the entire file upload workflow:
 * 1. Receive file from client
 * 2. Validate file (size, format, duplicates)
 * 3. Split into chunks
 * 4. Encrypt chunks
 * 5. Store chunks on remote servers
 * 6. Save metadata
 * 
 * Responsibilities:
 * - File reception and validation
 * - File-to-chunk splitting
 * - Chunk coordination (delegates actual storage/encryption)
 * - Metadata management
 * - Error handling and rollback
 * 
 * Dependencies:
 * - FileMetadataRepository (metadata persistence)
 * - ChunkMetadataRepository (chunk tracking)
 * - EncryptionService (chunk encryption)
 * - ChunkStorageService (remote chunk storage)
 * - CrcValidationService (chunk integrity)
 * - MetadataService (metadata operations)
 * 
 * Example:
 * <pre>
 * FileProcessingService fileService = new FileProcessingService(
 *     fileRepo, chunkRepo, encryptService, storageService, 
 *     crcService, metadataService
 * );
 * 
 * String fileId = fileService.processUpload(file, encryptionAlgo);
 * </pre>
 */
public class FileProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    
    // Configuration
    private static final int CHUNK_SIZE = 5 * 1024 * 1024;  // 5 MB chunks
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;  // 5 GB max
    
    private final FileMetadataRepository fileRepo;
    private final ChunkMetadataRepository chunkRepo;
    private final EncryptionService encryptionService;
    private final ChunkStorageService storageService;
    private final CrcValidationService crcService;
    private final MetadataService metadataService;
    
    /**
     * Constructor - Initialize service with dependencies
     * 
     * @param fileRepo FileMetadata repository
     * @param chunkRepo ChunkMetadata repository
     * @param encryptionService Encryption service
     * @param storageService Remote storage service
     * @param crcService CRC validation service
     * @param metadataService Metadata service
     */
    public FileProcessingService(
            FileMetadataRepository fileRepo,
            ChunkMetadataRepository chunkRepo,
            EncryptionService encryptionService,
            ChunkStorageService storageService,
            CrcValidationService crcService,
            MetadataService metadataService) {
        
        this.fileRepo = fileRepo;
        this.chunkRepo = chunkRepo;
        this.encryptionService = encryptionService;
        this.storageService = storageService;
        this.crcService = crcService;
        this.metadataService = metadataService;
    }
    
    /**
     * Process file upload end-to-end
     * 
     * Orchestrates:
     * 1. Validate file
     * 2. Generate file ID
     * 3. Split into chunks
     * 4. Encrypt and store each chunk
     * 5. Save file metadata
     * 6. Save chunk metadata
     * 
     * On error, rolls back all changes.
     * 
     * @param file File to upload
     * @param encryptionAlgo Encryption algorithm (AES/256/GCM, etc.)
     * @return File ID for future reference
     * @throws ProcessingException if upload fails at any step
     * @throws DatabaseException if metadata save fails
     */
    public String processUpload(File file, String encryptionAlgo) 
            throws ProcessingException, DatabaseException {
        
        logger.info("Processing file upload: {} ({} bytes)", 
                   file.getName(), file.length());
        
        // Validate input file
        validateFile(file);
        
        // Generate file ID
        String fileId = UUID.randomUUID().toString();
        logger.debug("Generated fileId: {}", fileId);
        
        try {
            // Create file metadata
            FileMetadata fileMetadata = new FileMetadata(
                fileId,
                file.getName(),
                calculateChunkCount(file.length()),
                file.length(),
                encryptionAlgo
            );
            
            // Save file metadata to database
            fileRepo.save(fileMetadata);
            logger.debug("✓ File metadata saved: {}", fileId);
            
            // Split and process chunks
            List<ChunkMetadata> chunkMetadataList = processChunks(
                fileId,
                file,
                encryptionAlgo
            );
            
            logger.info("✓ File upload completed successfully: {} ({} chunks)", 
                       fileId, chunkMetadataList.size());
            
            return fileId;
            
        } catch (ProcessingException e) {
            // Rollback on processing error
            logger.error("✗ File upload failed, rolling back: {}", e.getMessage());
            rollbackFileUpload(fileId);
            throw e;
            
        } catch (DatabaseException e) {
            // Rollback on database error
            logger.error("✗ Database error during upload, rolling back: {}", e.getMessage());
            rollbackFileUpload(fileId);
            throw e;
        }
    }
    
    /**
     * Get file by ID
     * 
     * Retrieves complete file information including all chunks.
     * 
     * @param fileId File identifier (UUID)
     * @return FileMetadata with associated chunks
     * @throws DatabaseException if query fails
     * @throws ProcessingException if file not found
     */
    public FileMetadata getFile(String fileId) 
            throws DatabaseException, ProcessingException {
        
        logger.debug("Retrieving file: {}", fileId);
        
        FileMetadata fileMetadata = fileRepo.findById(fileId);
        
        if (fileMetadata == null) {
            throw new ProcessingException(
                ProcessingException.ErrorType.FILE_NOT_FOUND,
                String.format("File not found: %s", fileId)
            );
        }
        
        logger.debug("✓ File retrieved: {}", fileId);
        return fileMetadata;
    }
    
    /**
     * Delete file and all associated chunks
     * 
     * Performs:
     * 1. Delete all chunk files from storage
     * 2. Delete chunk metadata from database
     * 3. Delete file metadata from database
     * 
     * @param fileId File identifier (UUID)
     * @throws DatabaseException if database operations fail
     * @throws ProcessingException if storage cleanup fails
     */
    public void deleteFile(String fileId) 
            throws DatabaseException, ProcessingException {
        
        logger.info("Deleting file: {}", fileId);
        
        try {
            // Get all chunks for the file
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByIndex(fileId);
            
            // Delete each chunk from storage
            for (ChunkMetadata chunk : chunks) {
                try {
                    storageService.deleteChunk(
                        chunk.getServerHost(),
                        chunk.getRemotePath()
                    );
                    logger.debug("✓ Chunk deleted from storage: {}[{}]", 
                               fileId, chunk.getChunkIndex());
                } catch (ProcessingException e) {
                    logger.warn("Failed to delete chunk from storage: {}[{}]", 
                              fileId, chunk.getChunkIndex(), e);
                    // Continue deletion even if storage cleanup fails
                }
            }
            
            // Delete chunk metadata from database
            chunkRepo.deleteByFileId(fileId);
            logger.debug("✓ Chunk metadata deleted: {}", fileId);
            
            // Delete file metadata from database
            fileRepo.deleteById(fileId);
            logger.info("✓ File deleted successfully: {}", fileId);
            
        } catch (DatabaseException e) {
            logger.error("✗ Database error during file deletion: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Validate file before upload
     * 
     * Checks:
     * - File exists
     * - File is readable
     * - File size within limits
     * - File is not empty
     * 
     * @param file File to validate
     * @throws ProcessingException if validation fails
     */
    private void validateFile(File file) throws ProcessingException {
        logger.debug("Validating file: {}", file.getName());
        
        // Check file exists
        if (!file.exists()) {
            throw new ProcessingException(
                ProcessingException.ErrorType.INVALID_FILE,
                "File does not exist: " + file.getName()
            );
        }
        
        // Check file is readable
        if (!file.canRead()) {
            throw new ProcessingException(
                ProcessingException.ErrorType.INVALID_FILE,
                "File is not readable: " + file.getName()
            );
        }
        
        // Check file is not empty
        if (file.length() == 0) {
            throw new ProcessingException(
                ProcessingException.ErrorType.INVALID_FILE,
                "File is empty: " + file.getName()
            );
        }
        
        // Check file size within limits
        if (file.length() > MAX_FILE_SIZE) {
            throw new ProcessingException(
                ProcessingException.ErrorType.FILE_TOO_LARGE,
                String.format("File exceeds maximum size (%d bytes): %s", 
                             MAX_FILE_SIZE, file.getName())
            );
        }
        
        logger.debug("✓ File validation passed: {}", file.getName());
    }
    
    /**
     * Split file into chunks and process
     * 
     * Orchestrates for each chunk:
     * 1. Read chunk from file
     * 2. Calculate CRC32 checksum
     * 3. Encrypt chunk
     * 4. Store on remote server
     * 5. Save chunk metadata
     * 
     * @param fileId File identifier
     * @param file File to chunk
     * @param encryptionAlgo Encryption algorithm
     * @return List of ChunkMetadata for all chunks
     * @throws ProcessingException if chunking fails
     * @throws DatabaseException if metadata save fails
     */
    private List<ChunkMetadata> processChunks(
            String fileId,
            File file,
            String encryptionAlgo) 
            throws ProcessingException, DatabaseException {
        
        logger.debug("Processing chunks for file: {}", fileId);
        
        List<ChunkMetadata> chunkMetadataList = new ArrayList<>();
        byte[] buffer = new byte[CHUNK_SIZE];
        
        try (FileInputStream fis = new FileInputStream(file)) {
            int chunkIndex = 0;
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) > 0) {
                logger.debug("Processing chunk: {}[{}] ({} bytes)", 
                           fileId, chunkIndex, bytesRead);
                
                // Extract chunk bytes
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                
                // Process single chunk
                ChunkMetadata chunkMetadata = processSingleChunk(
                    fileId,
                    chunkIndex,
                    chunkData,
                    encryptionAlgo
                );
                
                chunkMetadataList.add(chunkMetadata);
                chunkIndex++;
            }
            
            logger.debug("✓ All chunks processed: {} ({} chunks)", 
                        fileId, chunkMetadataList.size());
            
            return chunkMetadataList;
            
        } catch (IOException e) {
            logger.error("✗ Failed to read file: {}", e.getMessage(), e);
            throw new ProcessingException(
                ProcessingException.ErrorType.FILE_READ_ERROR,
                "Failed to read file: " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Process single chunk
     * 
     * Orchestrates:
     * 1. Calculate CRC32 checksum (before encryption)
     * 2. Encrypt chunk
     * 3. Store encrypted chunk on remote server
     * 4. Save chunk metadata
     * 
     * @param fileId File identifier
     * @param chunkIndex Zero-based chunk number
     * @param chunkData Raw chunk data (before encryption)
     * @param encryptionAlgo Encryption algorithm
     * @return ChunkMetadata with storage location and checksums
     * @throws ProcessingException if processing fails
     * @throws DatabaseException if metadata save fails
     */
    private ChunkMetadata processSingleChunk(
            String fileId,
            int chunkIndex,
            byte[] chunkData,
            String encryptionAlgo) 
            throws ProcessingException, DatabaseException {
        
        // Calculate CRC32 checksum of original data
        long crc32 = crcService.calculateCrc32(chunkData);
        logger.debug("CRC32 calculated: {}[{}] = {}", 
                    fileId, chunkIndex, crc32);
        
        // Encrypt chunk
        byte[] encryptedData = encryptionService.encrypt(chunkData, encryptionAlgo);
        logger.debug("Chunk encrypted: {}[{}] ({} → {} bytes)", 
                    fileId, chunkIndex, chunkData.length, encryptedData.length);
        
        // Store encrypted chunk on remote server
        String serverHost = storageService.selectServer();
        String remotePath = storageService.storeChunk(
            serverHost,
            fileId,
            chunkIndex,
            encryptedData
        );
        logger.debug("Chunk stored: {}[{}] @ {}:{}", 
                    fileId, chunkIndex, serverHost, remotePath);
        
        // Create chunk metadata
        ChunkMetadata chunkMetadata = new ChunkMetadata(
            fileId,
            chunkIndex,
            serverHost,
            remotePath,
            crc32,
            (long) chunkData.length
        );
        
        // Save chunk metadata
        chunkRepo.save(chunkMetadata);
        logger.debug("✓ Chunk metadata saved: {}[{}]", fileId, chunkIndex);
        
        return chunkMetadata;
    }
    
    /**
     * Calculate number of chunks needed
     * 
     * Calculates: ceil(fileSize / chunkSize)
     * 
     * @param fileSize File size in bytes
     * @return Number of chunks needed
     */
    private int calculateChunkCount(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }
    
    /**
     * Rollback file upload on error
     * 
     * Cleans up:
     * - Chunks stored on remote servers
     * - Chunk metadata in database
     * - File metadata in database
     * 
     * Best-effort cleanup - continues even if individual steps fail.
     * 
     * @param fileId File to rollback
     */
    private void rollbackFileUpload(String fileId) {
        logger.warn("Rolling back file upload: {}", fileId);
        
        try {
            // Get all chunks
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByIndex(fileId);
            
            // Delete each chunk from storage
            for (ChunkMetadata chunk : chunks) {
                try {
                    storageService.deleteChunk(
                        chunk.getServerHost(),
                        chunk.getRemotePath()
                    );
                } catch (Exception e) {
                    logger.warn("Failed to delete chunk during rollback: {}[{}]", 
                              fileId, chunk.getChunkIndex(), e);
                }
            }
            
            // Delete chunk metadata
            chunkRepo.deleteByFileId(fileId);
            
            // Delete file metadata
            fileRepo.deleteById(fileId);
            
            logger.info("✓ Rollback completed: {}", fileId);
            
        } catch (Exception e) {
            logger.error("✗ Rollback failed: {}", e.getMessage(), e);
            // Don't throw - rollback is best-effort
        }
    }
}
