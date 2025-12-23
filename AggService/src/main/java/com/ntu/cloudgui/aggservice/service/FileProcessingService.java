package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.DatabaseException;
import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import com.ntu.cloudgui.aggservice.repository.ChunkMetadataRepository;
import com.ntu.cloudgui.aggservice.repository.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FileProcessingService - File Upload and Chunking Orchestration.
 */
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
     */
    public String processUpload(File file, String encryptionAlgo)
            throws ProcessingException, DatabaseException {

        logger.info("Processing file upload: {} ({} bytes)", file.getName(), file.length());
        validateFile(file);

        String fileId = UUID.randomUUID().toString();
        logger.debug("Generated fileId: {}", fileId);

        try {
            FileMetadata fileMetadata = new FileMetadata(
                    fileId,
                    file.getName(),
                    calculateChunkCount(file.length()),
                    file.length(),
                    encryptionAlgo
            );

            fileRepo.save(fileMetadata);
            logger.debug("✓ File metadata saved: {}", fileId);

            List<ChunkMetadata> chunkMetadataList = processChunks(fileId, file, encryptionAlgo);

            logger.info("✓ File upload completed successfully: {} ({} chunks)",
                    fileId, chunkMetadataList.size());
            return fileId;

        } catch (ProcessingException e) {
            logger.error("✗ File upload failed, rolling back: {}", e.getMessage());
            rollbackFileUpload(fileId);
            throw e;
        } catch (DatabaseException e) {
            logger.error("✗ Database error during upload, rolling back: {}", e.getMessage());
            rollbackFileUpload(fileId);
            throw e;
        }
    }

    /**
     * Retrieve file metadata by ID.
     */
    public FileMetadata getFile(String fileId) throws DatabaseException, ProcessingException {
        logger.debug("Retrieving file: {}", fileId);

        FileMetadata fileMetadata = fileRepo.findById(fileId);
        if (fileMetadata == null) {
            throw new ProcessingException(
                    String.format("File not found: %s", fileId),
                    ErrorType.FILE_NOT_FOUND
            );
        }

        logger.debug("✓ File retrieved: {}", fileId);
        return fileMetadata;
    }

    /**
     * Delete file and all associated chunks.
     */
    public void deleteFile(String fileId) throws DatabaseException, ProcessingException {
        logger.info("Deleting file: {}", fileId);

        try {
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByIndex(fileId);

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

            chunkRepo.deleteByFileId(fileId);
            logger.debug("✓ Chunk metadata deleted: {}", fileId);

            fileRepo.deleteById(fileId);
            logger.info("✓ File deleted successfully: {}", fileId);

        } catch (DatabaseException e) {
            logger.error("✗ Database error during file deletion: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validate file before upload.
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
     */
    private List<ChunkMetadata> processChunks(String fileId,
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
        }
    }

    /**
     * Process a single chunk: CRC, encrypt, store, save metadata.
     */
    private ChunkMetadata processSingleChunk(String fileId,
                                             int chunkIndex,
                                             byte[] chunkData,
                                             String encryptionAlgo)
            throws ProcessingException, DatabaseException {

        long crc32 = crcService.calculateCrc32(chunkData);
        logger.debug("CRC32 calculated: {}[{}] = {}", fileId, chunkIndex, crc32);

        byte[] encryptedData = encryptionService.encrypt(chunkData, encryptionAlgo);
        logger.debug("Chunk encrypted: {}[{}] ({} → {} bytes)",
                fileId, chunkIndex, chunkData.length, encryptedData.length);

        String serverHost = storageService.selectServer();
        String remotePath = storageService.storeChunk(serverHost, fileId, chunkIndex, encryptedData);
        logger.debug("Chunk stored: {}[{}] @ {}:{}",
                fileId, chunkIndex, serverHost, remotePath);

        ChunkMetadata chunkMetadata = new ChunkMetadata(
                fileId,
                chunkIndex,
                serverHost,
                remotePath,
                crc32,
                (long) chunkData.length
        );

        chunkRepo.save(chunkMetadata);
        logger.debug("✓ Chunk metadata saved: {}[{}]", fileId, chunkIndex);

        return chunkMetadata;
    }

    /**
     * Calculate number of chunks needed: ceil(fileSize / CHUNK_SIZE).
     */
    private int calculateChunkCount(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }

    /**
     * Best-effort rollback after a failed upload.
     */
    private void rollbackFileUpload(String fileId) {
        logger.warn("Rolling back file upload: {}", fileId);
        try {
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByIndex(fileId);

            for (ChunkMetadata chunk : chunks) {
                try {
                    storageService.deleteChunk(chunk.getServerHost(), chunk.getRemotePath());
                } catch (Exception e) {
                    logger.warn("Failed to delete chunk during rollback: {}[{}]",
                            fileId, chunk.getChunkIndex(), e);
                }
            }

            chunkRepo.deleteByFileId(fileId);
            fileRepo.deleteById(fileId);

            logger.info("✓ Rollback completed: {}", fileId);
        } catch (Exception e) {
            logger.error("✗ Rollback failed: {}", e.getMessage(), e);
        }
    }
}
