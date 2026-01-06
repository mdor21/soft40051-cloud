package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.ErrorType;
import com.ntu.cloudgui.aggservice.exception.ProcessingException;
import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.model.FileMetadata;
import com.ntu.cloudgui.aggservice.repository.ChunkMetadataDao;
import com.ntu.cloudgui.aggservice.repository.FileMetadataDao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class FileProcessingService {

    private static final int CHUNK_SIZE = 5 * 1024 * 1024;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;

    private final Semaphore fileOperationSemaphore;

    private final FileMetadataDao fileRepo;
    private final ChunkMetadataDao chunkRepo;
    private final EncryptionService encryptionService;
    private final ChunkStorageService storageService;
    private final CrcValidationService crcService;
    private final DatabaseLoggingService dbLogger;

    public FileProcessingService(FileMetadataDao fileRepo,
                                 ChunkMetadataDao chunkRepo,
                                 EncryptionService encryptionService,
                                 ChunkStorageService storageService,
                                 CrcValidationService crcService,
                                 DatabaseLoggingService dbLogger,
                                 int semaphorePermits) {
        this.fileRepo = fileRepo;
        this.chunkRepo = chunkRepo;
        this.encryptionService = encryptionService;
        this.storageService = storageService;
        this.crcService = crcService;
        this.dbLogger = dbLogger;
        this.fileOperationSemaphore = new Semaphore(semaphorePermits);
    }

    public String processUpload(File file, String encryptionAlgo)
            throws ProcessingException {

        System.out.println("Attempting to acquire lock for file upload: " + file.getName());
        dbLogger.info(String.format("Upload initiated for file: %s", file.getName()));
        try {
            fileOperationSemaphore.acquire();
            System.out.println("Lock acquired for file upload: " + file.getName());

            System.out.println("Processing file upload: " + file.getName() + " (" + file.length() + " bytes)");
            validateFile(file);

            String fileId = UUID.randomUUID().toString();

            try {
                FileMetadata fileMetadata = new FileMetadata(
                        fileId,
                        file.getName(),
                        calculateChunkCount(file.length()),
                        file.length(),
                        encryptionAlgo,
                        LocalDateTime.now()
                );

                fileRepo.save(fileMetadata);

                List<ChunkMetadata> chunkMetadataList = processChunks(fileId, file, encryptionAlgo);

                System.out.println("✓ File upload completed successfully: " + fileId + " (" + chunkMetadataList.size() + " chunks)");
                dbLogger.info(String.format("File upload successful for %s. File ID: %s", file.getName(), fileId));
                return fileId;

            } catch (Exception e) {
                System.err.println("✗ File upload failed, rolling back: " + e.getMessage());
                dbLogger.error(String.format("File upload failed for %s: %s", file.getName(), e.getMessage()));
                rollbackFileUpload(fileId);
                throw new ProcessingException(
                        "Unexpected error during file processing: " + e.getMessage(),
                        ErrorType.PROCESSING_ERROR,
                        e
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dbLogger.error("File upload was interrupted while waiting for lock for file: " + file.getName());
            throw new ProcessingException("File upload was interrupted while waiting for lock", ErrorType.PROCESSING_ERROR, e);
        } finally {
            fileOperationSemaphore.release();
            System.out.println("Lock released for file upload: " + file.getName());
        }
    }

    public byte[] processDownload(String fileId, String encryptionAlgo) throws ProcessingException {
        System.out.println("Attempting to acquire lock for file download: " + fileId);
        dbLogger.info(String.format("Download initiated for file ID: %s", fileId));
        try {
            fileOperationSemaphore.acquire();
            System.out.println("Lock acquired for file download: " + fileId);

            FileMetadata fileMetadata = getFile(fileId);
            List<ChunkMetadata> chunkMetadataList = chunkRepo.findByFileIdOrderByChunkIndex(fileId);

            try (ByteArrayOutputStream reassembledFile = new ByteArrayOutputStream()) {
                for (ChunkMetadata chunkMetadata : chunkMetadataList) {
                    byte[] encryptedChunk = storageService.retrieveChunk(chunkMetadata.getServerHost(), chunkMetadata.getRemotePath());
                    byte[] decryptedChunk = encryptionService.decrypt(encryptedChunk, encryptionAlgo);

                    long crc32 = crcService.calculateCrc32(decryptedChunk);
                    if (crc32 != chunkMetadata.getCrc32()) {
                        String errorMsg = String.format("CRC32 checksum mismatch for file %s, chunk %d", fileId, chunkMetadata.getChunkIndex());
                        dbLogger.error(errorMsg);
                        throw new ProcessingException(errorMsg, ErrorType.VALIDATION_ERROR);
                    }

                    reassembledFile.write(decryptedChunk);
                }
                System.out.println("✓ File download completed successfully: " + fileId);
                dbLogger.info(String.format("File download successful for file ID: %s", fileId));
                return reassembledFile.toByteArray();
            } catch (IOException e) {
                String errorMsg = "Failed to reassemble file: " + e.getMessage();
                dbLogger.error(errorMsg);
                throw new ProcessingException(errorMsg, ErrorType.PROCESSING_ERROR, e);
            }
        } catch (InterruptedException | SQLException e) {
            Thread.currentThread().interrupt();
            dbLogger.error("File download was interrupted while waiting for lock for file ID: " + fileId);
            throw new ProcessingException("File download was interrupted while waiting for lock", ErrorType.PROCESSING_ERROR, e);
        } finally {
            fileOperationSemaphore.release();
            System.out.println("Lock released for file download: " + fileId);
        }
    }

    public FileMetadata getFile(String fileId) throws ProcessingException {
        try {
            return fileRepo.findById(fileId).orElseThrow(() -> new ProcessingException(String.format("File not found: %s", fileId), ErrorType.FILE_NOT_FOUND));
        } catch (SQLException e) {
            System.err.println("Database error retrieving file: " + e.getMessage());
            throw new ProcessingException("Failed to retrieve file metadata: " + e.getMessage(), ErrorType.PROCESSING_ERROR, e);
        }
    }

    public void deleteFile(String fileId) throws ProcessingException {
        System.out.println("Attempting to acquire lock for file deletion: " + fileId);
        dbLogger.info(String.format("Deletion initiated for file ID: %s", fileId));
        try {
            fileOperationSemaphore.acquire();
            System.out.println("Lock acquired for file deletion: " + fileId);
            System.out.println("Deleting file: " + fileId);
            try {
                List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByChunkIndex(fileId);
                for (ChunkMetadata chunk : chunks) {
                    try {
                        storageService.deleteChunk(chunk.getServerHost(), chunk.getRemotePath());
                    } catch (ProcessingException e) {
                        System.err.println("Failed to delete chunk from storage: " + fileId + "[" + chunk.getChunkIndex() + "]");
                    }
                }
                chunkRepo.deleteByFileId(fileId);
                fileRepo.deleteById(fileId);
                System.out.println("✓ File deleted successfully: " + fileId);
                dbLogger.info(String.format("File deletion successful for file ID: %s", fileId));
            } catch (Exception e) {
                System.err.println("✗ Unexpected error during file deletion: " + e.getMessage());
                dbLogger.error(String.format("Unexpected error during file deletion for %s: %s", fileId, e.getMessage()));
                throw new ProcessingException("Failed to delete file: " + e.getMessage(), ErrorType.PROCESSING_ERROR, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dbLogger.error("File deletion was interrupted while waiting for lock for file ID: " + fileId);
            throw new ProcessingException("File deletion was interrupted while waiting for lock", ErrorType.PROCESSING_ERROR, e);
        } finally {
            fileOperationSemaphore.release();
            System.out.println("Lock released for file deletion: " + fileId);
        }
    }

    private void validateFile(File file) throws ProcessingException {
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
    }

    private List<ChunkMetadata> processChunks(String fileId, File file, String encryptionAlgo) throws ProcessingException {
        List<ChunkMetadata> chunkMetadataList = new ArrayList<>();
        byte[] buffer = new byte[CHUNK_SIZE];
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
                    UUID.randomUUID().toString(),
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
            return chunkMetadata;
        } catch (Exception e) {
            throw new ProcessingException(String.format("Failed to process chunk %d: %s", chunkIndex, e.getMessage()), ErrorType.PROCESSING_ERROR, e);
        }
    }

    private int calculateChunkCount(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }

    private void rollbackFileUpload(String fileId) {
        System.err.println("Rolling back file upload: " + fileId);
        try {
            List<ChunkMetadata> chunks = chunkRepo.findByFileIdOrderByChunkIndex(fileId);
            for (ChunkMetadata chunk : chunks) {
                try {
                    storageService.deleteChunk(chunk.getServerHost(), chunk.getRemotePath());
                } catch (Exception e) {
                    System.err.println("Failed to delete chunk during rollback: " + fileId + "[" + chunk.getChunkIndex() + "]");
                }
            }
            chunkRepo.deleteByFileId(fileId);
            fileRepo.deleteById(fileId);
            System.out.println("✓ Rollback completed: " + fileId);
        } catch (Exception e) {
            System.err.println("✗ Rollback failed: " + e.getMessage());
        }
    }
}
