package com.ntu.cloudgui.aggservice;

import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private final int chunkSize;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5 GB

    private final Semaphore fileOperationSemaphore;

    private final FileMetadataRepository fileRepo;
    private final ChunkMetadataRepository chunkRepo;
    private final EncryptionService encryptionService;
    private final ChunkStorageService storageService;
    private final CrcValidationService crcService;

    public FileProcessingService(FileMetadataRepository fileRepo,
                                 ChunkMetadataRepository chunkRepo,
                                 EncryptionService encryptionService,
                                 ChunkStorageService storageService,
                                 CrcValidationService crcService,
                                 int semaphorePermits,
                                 int chunkSize) {
        this.fileRepo = fileRepo;
        this.chunkRepo = chunkRepo;
        this.encryptionService = encryptionService;
        this.storageService = storageService;
        this.crcService = crcService;
        this.fileOperationSemaphore = new Semaphore(semaphorePermits);
        this.chunkSize = chunkSize;
    }

    public String processUpload(File file, String encryptionAlgo)
            throws ProcessingException {
        logger.info("Attempting to acquire lock for file upload: {}", file.getName());
        try {
            fileOperationSemaphore.acquire();
            logger.info("Lock acquired for file upload: {}", file.getName());

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

            } catch (Exception e) {
                logger.error("✗ File upload failed, rolling back: {}", e.getMessage(), e);
                rollbackFileUpload(fileId);
                throw new ProcessingException(
                        "Unexpected error during file processing: " + e.getMessage(),
                        ErrorType.PROCESSING_ERROR,
                        e
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("File upload was interrupted while waiting for lock", ErrorType.PROCESSING_ERROR, e);
        } finally {
            fileOperationSemaphore.release();
            logger.info("Lock released for file upload: {}", file.getName());
        }
    }

    public byte[] processDownload(String fileId, String encryptionAlgo) throws ProcessingException {
        logger.info("Attempting to acquire lock for file download: {}", fileId);
        try {
            fileOperationSemaphore.acquire();
            logger.info("Lock acquired for file download: {}", fileId);

            FileMetadata fileMetadata = getFile(fileId);
            List<ChunkMetadata> chunkMetadataList = chunkRepo.findByFileIdOrderByChunkIndex(fileId);

            try (ByteArrayOutputStream reassembledFile = new ByteArrayOutputStream()) {
                for (ChunkMetadata chunkMetadata : chunkMetadataList) {
                    byte[] encryptedChunk = storageService.retrieveChunk(chunkMetadata.getServerHost(), chunkMetadata.getRemotePath());
                    byte[] decryptedChunk = encryptionService.decrypt(encryptedChunk, encryptionAlgo);

                    long crc32 = crcService.calculateCrc32(decryptedChunk);
                    if (crc32 != chunkMetadata.getCrc32()) {
                        throw new ProcessingException("CRC32 checksum mismatch for chunk " + chunkMetadata.getChunkIndex(), ErrorType.VALIDATION_ERROR);
                    }

                    reassembledFile.write(decryptedChunk);
                }
                logger.info("✓ File download completed successfully: {}", fileId);
                return reassembledFile.toByteArray();
            } catch (IOException e) {
                throw new ProcessingException("Failed to reassemble file: " + e.getMessage(), ErrorType.PROCESSING_ERROR, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessingException("File download was interrupted while waiting for lock", ErrorType.PROCESSING_ERROR, e);
        } finally {
            fileOperationSemaphore.release();
            logger.info("Lock released for file download: {}", fileId);
        }
    }

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
        } catch (Exception e) {
            throw new ProcessingException(
                    "Failed to retrieve file metadata: " + e.getMessage(),
                    ErrorType.PROCESSING_ERROR,
                    e
            );
        }
    }

    private void validateFile(File file) throws ProcessingException {
        logger.debug("Validating file: {}", file.getName());
        if (!file.exists()) {
            throw new ProcessingException("File does not exist: " + file.getName(), ErrorType.INVALID_FILE);
        }
        if (!file.canRead()) {
            throw new ProcessingException("File is not readable: " + file.getName(), ErrorType.INVALID_FILE);
        }
        if (file.length() == 0) {
            throw new ProcessingException("File is empty: " + file.getName(), ErrorType.INVALID_FILE);
        }
        if (file.length() > MAX_FILE_SIZE) {
            throw new ProcessingException(String.format("File exceeds maximum size (%d bytes): %s", MAX_FILE_SIZE, file.getName()), ErrorType.FILE_TOO_LARGE);
        }
        logger.debug("✓ File validation passed: {}", file.getName());
    }

    private int calculateChunkCount(long fileSize) {
        return (int) Math.ceil((double) fileSize / chunkSize);
    }

    private List<ChunkMetadata> processChunks(String fileId, File file, String encryptionAlgo) throws ProcessingException {
        logger.debug("Processing chunks for file: {}", fileId);
        List<ChunkMetadata> chunkMetadataList = new ArrayList<>();
        byte[] buffer = new byte[chunkSize];
        try (FileInputStream fis = new FileInputStream(file)) {
            int chunkIndex = 0;
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                ChunkMetadata chunkMetadata = processSingleChunk(fileId, chunkIndex, chunkData, encryptionAlgo);
                chunkMetadataList.add(chunkMetadata);
                chunkIndex++;
            }
            logger.debug("✓ All chunks processed: {} ({} chunks)", fileId, chunkMetadataList.size());
            return chunkMetadataList;
        } catch (IOException e) {
            throw new ProcessingException("Failed to read file: " + e.getMessage(), ErrorType.FILE_READ_ERROR, e);
        }
    }

    private ChunkMetadata processSingleChunk(String fileId, int chunkIndex, byte[] chunkData, String encryptionAlgo) throws ProcessingException {
        try {
            long crc32 = crcService.calculateCrc32(chunkData);
            byte[] encryptedData = encryptionService.encrypt(chunkData, encryptionAlgo);
            String serverHost = storageService.selectServer();
            String remotePath = storageService.storeChunk(serverHost, fileId, chunkIndex, encryptedData);
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
        } catch (Exception e) {
            throw new ProcessingException(String.format("Failed to process chunk %d: %s", chunkIndex, e.getMessage()), ErrorType.PROCESSING_ERROR, e);
        }
    }

    private void rollbackFileUpload(String fileId) {
        logger.warn("Rolling back file upload: {}", fileId);
        try {
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByChunkIndex(fileId);
            for (ChunkMetadata chunk : chunks) {
                try {
                    storageService.deleteChunk(chunk.getServerHost(), chunk.getRemotePath());
                } catch (Exception e) {
                    logger.warn("Failed to delete chunk during rollback: {}[{}]", fileId, chunk.getChunkIndex(), e);
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
