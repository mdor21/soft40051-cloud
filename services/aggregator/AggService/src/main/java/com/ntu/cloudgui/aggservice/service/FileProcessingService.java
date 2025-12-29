package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import com.ntu.cloudgui.aggservice.repository.ChunkMetadataRepository;
import com.ntu.cloudgui.aggservice.repository.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FileProcessingService - File Upload and Chunking Orchestration.
 *
 * Orchestrates the complete file upload workflow:
 * 1. Validate file
 * 2. Generate fileId (UUID)
 * 3. Save file metadata to database
 * 4. Read file and split into chunks
 * 5. For each chunk: CRC, encrypt, store, save metadata
 * 6. Handle errors with automatic rollback
 *
 * Chunk Size: 5 MB (5242880 bytes)
 * Max File Size: 5 GB
 * Encryption: AES-256
 */
@Service
public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private static final int CHUNK_SIZE = 5 * 1024 * 1024;                 // 5 MB
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;     // 5 GB

    private final FileMetadataRepository fileRepo;
    private final ChunkMetadataRepository chunkRepo;
    private final EncryptionService encryptionService;
    private final ChunkStorageService storageService;
    private final CrcValidationService crcService;
    private final MetadataService metadataService;

    public FileProcessingService(FileMetadataRepository fileRepo,
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
     * Process file upload end-to-end.
     *
     * Complete workflow:
     * 1. Validate file exists and is readable
     * 2. Check file size is within limits
     * 3. Generate unique fileId (UUID)
     * 4. Save file metadata to database
     * 5. Read file in 5MB chunks
     * 6. For each chunk:
     *    a. Calculate CRC32 checksum
     *    b. Encrypt with AES-256
     *    c. Select server (round-robin)
     *    d. Store encrypted chunk via SFTP
     *    e. Save chunk metadata to database
     * 7. Return fileId to client
     *
     * On error: Automatic rollback deletes all stored chunks and metadata.
     *
     * @param file File object to process
     * @param encryptionAlgo Encryption algorithm (e.g., "AES")
     * @return Generated fileId (UUID)
     * @throws ProcessingException if file processing fails
     */
    public String processUpload(File file, String encryptionAlgo)
            throws ProcessingException {

        logger.info("Processing file upload: {} ({} bytes)", file.getName(), file.length());
        validateFile(file);

        String fileId = UUID.randomUUID().toString();
        logger.debug("Generated fileId: {}", fileId);

        try {
            // 1. Create and save file metadata
            FileMetadata fileMetadata = new FileMetadata(
                    fileId,
                    file.getName(),
                    calculateChunkCount(file.length()),
                    file.length(),
                    encryptionAlgo
            );

            fileRepo.save(fileMetadata);
            logger.debug("✓ File metadata saved: {}", fileId);

            // 2. Process all chunks
            List<ChunkMetadata> chunkMetadataList = processChunks(fileId, file, encryptionAlgo);

            logger.info("✓ File upload completed successfully: {} ({} chunks)",
                    fileId, chunkMetadataList.size());
            return fileId;

        } catch (ProcessingException e) {
            logger.error("✗ File upload failed, rolling back: {}", e.getMessage());
            rollbackFileUpload(fileId);
            throw e;
        } catch (Exception e) {
            logger.error("✗ Unexpected error during upload, rolling back: {}", e.getMessage(), e);
            rollbackFileUpload(fileId);
            throw new ProcessingException(
                    "Unexpected error during file processing: " + e.getMessage(),
                    ErrorType.PROCESSING_ERROR,
                    e
            );
        }
    }

    /**
     * Retrieve file metadata by ID.
     *
     * Used before download to get file information:
     * - Original filename
     * - Total chunk count
     * - File size
     * - Encryption algorithm
     *
     * @param fileId Unique file identifier (UUID)
     * @return FileMetadata object
     * @throws ProcessingException if file not found or database error
     */
    public FileMetadata getFile(String fileId) throws ProcessingException {
        logger.debug("Retrieving file: {}", fileId);

        try {
            FileMetadata fileMetadata = fileRepo.findById(fileId);
            if (fileMetadata == null) {
                throw new ProcessingException(
                        String.format("File not found: %s", fileId),
                        ErrorType.FILE_NOT_FOUND
                );
            }

            logger.debug("✓ File retrieved: {}", fileId);
            return fileMetadata;

        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Database error retrieving file: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "Failed to retrieve file metadata: " + e.getMessage(),
                    ErrorType.PROCESSING_ERROR,
                    e
            );
        }
    }

    /**
     * Delete file and all associated chunks.
     *
     * Process:
     * 1. Fetch all chunk metadata for file
     * 2. Delete each encrypted chunk from storage servers (SFTP)
     * 3. Delete all chunk metadata from database
     * 4. Delete file metadata from database
     *
     * Best-effort approach: Continues even if individual chunk deletion fails.
     * Logs warnings for failed deletions.
     *
     * @param fileId Unique file identifier (UUID)
     * @throws ProcessingException if critical error occurs
     */
    public void deleteFile(String fileId) throws ProcessingException {
        logger.info("Deleting file: {}", fileId);

        try {
            // 1. Fetch all chunks for this file
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByChunkIndex(fileId);

            // 2. Delete chunks from storage servers
            for (ChunkMetadata chunk : chunks) {
                try {
                    storageService.deleteChunk(chunk.getServerHost(), chunk.getRemotePath());
                    logger.debug("✓ Chunk deleted from storage: {}[{}]",
                            fileId, chunk.getChunkIndex());
                } catch (ProcessingException e) {
                    logger.warn("Failed to delete chunk from storage: {}[{}]",
                            fileId, chunk.getChunkIndex(), e);
                }
            }

            // 3. Delete chunk metadata from database
            chunkRepo.deleteByFileId(fileId);
            logger.debug("✓ Chunk metadata deleted: {}", fileId);

            // 4. Delete file metadata from database
            fileRepo.deleteById(fileId);
            logger.info("✓ File deleted successfully: {}", fileId);

        } catch (ProcessingException e) {
            logger.error("✗ Processing error during file deletion: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("✗ Unexpected error during file deletion: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "Failed to delete file: " + e.getMessage(),
                    ErrorType.PROCESSING_ERROR,
                    e
            );
        }
    }

    /**
     * Validate file before upload.
     *
     * Checks:
     * - File exists
     * - File is readable
     * - File is not empty
     * - File size within 5GB limit
     *
     * @param file File to validate
     * @throws ProcessingException if validation fails
     */
    private void validateFile(File file) throws ProcessingException {
        logger.debug("Validating file: {}", file.getName());

        if (!file.exists()) {
            throw new ProcessingException(
                    "File does not exist: " + file.getName(),
                    ErrorType.INVALID_FILE
            );
        }

        if (!file.canRead()) {
            throw new ProcessingException(
                    "File is not readable: " + file.getName(),
                    ErrorType.INVALID_FILE
            );
        }

        if (file.length() == 0) {
            throw new ProcessingException(
                    "File is empty: " + file.getName(),
                    ErrorType.INVALID_FILE
            );
        }

        if (file.length() > MAX_FILE_SIZE) {
            throw new ProcessingException(
                    String.format("File exceeds maximum size (%d bytes): %s",
                            MAX_FILE_SIZE, file.getName()),
                    ErrorType.FILE_TOO_LARGE
            );
        }

        logger.debug("✓ File validation passed: {}", file.getName());
    }

    /**
     * Split file into chunks and process them.
     *
     * Process:
     * 1. Open file input stream
     * 2. Read 5MB chunks
     * 3. For each chunk: CRC, encrypt, store, save metadata
     * 4. Handle last chunk (may be < 5MB)
     *
     * @param fileId Unique file identifier
     * @param file File to chunk
     * @param encryptionAlgo Encryption algorithm
     * @return List of created ChunkMetadata objects
     * @throws ProcessingException if chunking fails
     */
    private List<ChunkMetadata> processChunks(String fileId,
                                              File file,
                                              String encryptionAlgo)
            throws ProcessingException {

        logger.debug("Processing chunks for file: {}", fileId);

        List<ChunkMetadata> chunkMetadataList = new ArrayList<>();
        byte[] buffer = new byte[CHUNK_SIZE];

        try (FileInputStream fis = new FileInputStream(file)) {
            int chunkIndex = 0;
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                logger.debug("Processing chunk: {}[{}] ({} bytes)",
                        fileId, chunkIndex, bytesRead);

                // Trim buffer to actual bytes read (last chunk may be smaller)
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

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
                    "Failed to read file: " + e.getMessage(),
                    ErrorType.FILE_READ_ERROR,
                    e
            );
        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("✗ Unexpected error during chunking: {}", e.getMessage(), e);
            throw new ProcessingException(
                    "Unexpected error during chunking: " + e.getMessage(),
                    ErrorType.PROCESSING_ERROR,
                    e
            );
        }
    }

    /**
     * Process a single chunk: CRC, encrypt, store, save metadata.
     *
     * Process:
     * 1. Calculate CRC32 checksum of plaintext
     * 2. Encrypt chunk with AES-256
     * 3. Select storage server (round-robin)
     * 4. Upload encrypted chunk via SFTP
     * 5. Save chunk metadata to database
     *
     * @param fileId Unique file identifier
     * @param chunkIndex Index of chunk in sequence
     * @param chunkData Plaintext chunk data
     * @param encryptionAlgo Encryption algorithm
     * @return ChunkMetadata object
     * @throws ProcessingException if chunk processing fails
     */
    private ChunkMetadata processSingleChunk(String fileId,
                                             int chunkIndex,
                                             byte[] chunkData,
                                             String encryptionAlgo)
            throws ProcessingException {

        try {
            // 1. Calculate CRC32 checksum of plaintext
            long crc32 = crcService.calculateCrc32(chunkData);
            logger.debug("CRC32 calculated: {}[{}] = {}", fileId, chunkIndex, crc32);

            // 2. Encrypt chunk
            byte[] encryptedData = encryptionService.encrypt(chunkData, encryptionAlgo);
            logger.debug("Chunk encrypted: {}[{}] ({} → {} bytes)",
                    fileId, chunkIndex, chunkData.length, encryptedData.length);

            // 3. Select server and store chunk
            String serverHost = storageService.selectServer();
            String remotePath = storageService.storeChunk(serverHost, fileId, chunkIndex, encryptedData);
            logger.debug("Chunk stored: {}[{}] @ {}:{}",
                    fileId, chunkIndex, serverHost, remotePath);

            // 4. Create and save chunk metadata
            ChunkMetadata chunkMetadata = new ChunkMetadata(
                    fileId,
                    chunkIndex,
                    serverHost,
                    remotePath,
                    (long) chunkData.length,
                    (long) encryptedData.length,
                    crc32,
                    LocalDateTime.now()
            );

            chunkRepo.save(chunkMetadata);
            logger.debug("✓ Chunk metadata saved: {}[{}]", fileId, chunkIndex);

            return chunkMetadata;

        } catch (ProcessingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("✗ Failed to process chunk: {}[{}] - {}", fileId, chunkIndex, e.getMessage(), e);
            throw new ProcessingException(
                    String.format("Failed to process chunk %d: %s", chunkIndex, e.getMessage()),
                    ErrorType.PROCESSING_ERROR,
                    e
            );
        }
    }

    /**
     * Calculate number of chunks needed for file.
     *
     * Formula: ceil(fileSize / CHUNK_SIZE)
     * Example: 15MB file = 3 chunks (5MB + 5MB + 5MB)
     *
     * @param fileSize Total file size in bytes
     * @return Number of chunks needed
     */
    private int calculateChunkCount(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }

    /**
     * Best-effort rollback after failed upload.
     *
     * Deletes:
     * 1. All stored chunks from storage servers
     * 2. All chunk metadata from database
     * 3. File metadata from database
     *
     * Logs warnings if individual operations fail.
     * Does NOT throw exceptions to allow caller to throw original error.
     *
     * @param fileId Unique file identifier to rollback
     */
    private void rollbackFileUpload(String fileId) {
        logger.warn("Rolling back file upload: {}", fileId);
        try {
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByChunkIndex(fileId);

            // Delete chunks from storage servers
            for (ChunkMetadata chunk : chunks) {
                try {
                    storageService.deleteChunk(chunk.getServerHost(), chunk.getRemotePath());
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
        }
    }
}
