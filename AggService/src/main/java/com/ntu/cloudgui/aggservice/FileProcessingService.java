package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private final EncryptionService encryptionService;
    private final ChunkStorageService chunkStorageService;
    private final CrcValidationService crcValidationService;
    private final FileMetadataRepository fileMetadataRepository;
    private final ChunkMetadataRepository chunkMetadataRepository;
    private final DatabaseLoggingService loggingService;
    private final int chunkSize;

    public FileProcessingService(Configuration config, EncryptionService encryptionService, ChunkStorageService chunkStorageService, CrcValidationService crcValidationService, FileMetadataRepository fileMetadataRepository, ChunkMetadataRepository chunkMetadataRepository, DatabaseLoggingService loggingService) {
        this.chunkSize = config.getChunkSize();
        this.encryptionService = encryptionService;
        this.chunkStorageService = chunkStorageService;
        this.crcValidationService = crcValidationService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.chunkMetadataRepository = chunkMetadataRepository;
        this.loggingService = loggingService;
    }

    public String processAndStoreFile(String filename, byte[] fileData, String username) throws ProcessingException {
        return processAndStoreFile(filename, fileData, username, null);
    }

    public String processAndStoreFile(String filename, byte[] fileData, String username, String fileId) throws ProcessingException {
        String effectiveUsername = normalizeUsername(username);
        String effectiveFileId = normalizeFileId(fileId);
        try {
            // 1. Encrypt the entire file
            byte[] encryptedData = encryptionService.encrypt(fileData);
            loggingService.logEvent(effectiveUsername, "FILE_ENCRYPTION", "File '" + filename + "' encrypted successfully.", LogEntry.Status.SUCCESS);

            // 2. Save initial file metadata
            int totalChunks = (int) Math.ceil((double) encryptedData.length / chunkSize);
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setFileId(effectiveFileId);
            fileMetadata.setOriginalFilename(filename);
            fileMetadata.setFileSize(fileData.length);
            fileMetadata.setTotalChunks(totalChunks);
            fileMetadata.setUsername(effectiveUsername);
            fileMetadataRepository.save(fileMetadata);
            loggingService.logEvent(effectiveUsername, "METADATA_PERSIST", "Initial metadata for file ID " + effectiveFileId + " saved.", LogEntry.Status.SUCCESS);

            // 3. Chunk the encrypted data and process each chunk
            int chunkIndex = 0;
            for (int offset = 0; offset < encryptedData.length; offset += chunkSize) {
                int length = Math.min(chunkSize, encryptedData.length - offset);
                byte[] chunkBytes = Arrays.copyOfRange(encryptedData, offset, offset + length);

                long crc32 = crcValidationService.calculateCrc32(chunkBytes);
                String server = chunkStorageService.storeChunk(chunkBytes, effectiveFileId, chunkIndex);

                ChunkMetadata chunkMetadata = new ChunkMetadata();
                chunkMetadata.setFileId(effectiveFileId);
                chunkMetadata.setChunkIndex(chunkIndex);
                chunkMetadata.setCrc32(crc32);
                chunkMetadata.setFileServerName(server);
                chunkMetadata.setChunkPath(String.format("/data/%s/chunk_%d.enc", effectiveFileId, chunkIndex));
                chunkMetadata.setChunkSize(length);
                chunkMetadataRepository.save(chunkMetadata);

                chunkIndex++;
            }
            loggingService.logEvent(effectiveUsername, "FILE_UPLOAD_COMPLETE", "File '" + filename + "' (ID: " + effectiveFileId + ") uploaded successfully.", LogEntry.Status.SUCCESS);
            return effectiveFileId;

        } catch (SQLException | ProcessingException e) {
            String errorMsg = "Failed to process and store file '" + filename + "'";
            logger.error(errorMsg, e);
            loggingService.logEvent(effectiveUsername, "FILE_UPLOAD_FAILURE", errorMsg + ": " + e.getMessage(), LogEntry.Status.FAILURE);
            if (effectiveFileId != null) {
                rollbackFileCreation(effectiveFileId, effectiveUsername);
            }
            throw new ProcessingException(errorMsg, e);
        }
    }

    private void rollbackFileCreation(String fileId, String username) {
        try {
            // Delete all chunks from file servers
            List<ChunkMetadata> chunks = chunkMetadataRepository.findByFileIdOrderByChunkIndexAsc(fileId);
            for (ChunkMetadata chunk : chunks) {
                chunkStorageService.deleteChunk(chunk.getFileServerName(), fileId, chunk.getChunkIndex());
            }

            // Delete chunk metadata
            chunkMetadataRepository.deleteByFileId(fileId);

            // Delete file metadata
            fileMetadataRepository.deleteByFileId(fileId);

            loggingService.logEvent(username, "ROLLBACK_SUCCESS", "Successfully rolled back file creation for file ID " + fileId, LogEntry.Status.SUCCESS);
        } catch (SQLException | ProcessingException e) {
            String errorMsg = "Failed to rollback file creation for file ID " + fileId;
            logger.error(errorMsg, e);
            loggingService.logEvent(username, "ROLLBACK_FAILURE", errorMsg + ": " + e.getMessage(), LogEntry.Status.FAILURE);
        }
    }

    public byte[] retrieveAndReassembleFile(String fileId, String username) throws ProcessingException {
        String effectiveUsername = normalizeUsername(username);
        try {
            List<ChunkMetadata> chunkMetadatas = chunkMetadataRepository.findByFileIdOrderByChunkIndexAsc(fileId);
            if (chunkMetadatas.isEmpty()) {
                throw new ProcessingException("File not found or has no associated chunks for ID: " + fileId);
            }

            ByteArrayOutputStream reassembledEncryptedStream = new ByteArrayOutputStream();
            for (ChunkMetadata chunk : chunkMetadatas) {
                byte[] chunkBytes = chunkStorageService.retrieveChunk(chunk.getFileServerName(), fileId, chunk.getChunkIndex());

                if (!crcValidationService.validateCrc32(chunkBytes, chunk.getCrc32())) {
                    String errorMsg = "CRC32 check failed for chunk " + chunk.getChunkIndex() + " of file ID " + fileId;
                    loggingService.logEvent(effectiveUsername, "CRC32_VALIDATION_FAILURE", errorMsg, LogEntry.Status.FAILURE);
                    throw new ProcessingException(errorMsg);
                }

                reassembledEncryptedStream.write(chunkBytes);
            }

            byte[] decryptedData = encryptionService.decrypt(reassembledEncryptedStream.toByteArray());
            loggingService.logEvent(effectiveUsername, "FILE_DOWNLOAD_COMPLETE", "File ID " + fileId + " retrieved successfully.", LogEntry.Status.SUCCESS);

            return decryptedData;

        } catch (SQLException | IOException | ProcessingException e) {
            String errorMsg = "Failed to retrieve and reassemble file ID " + fileId;
            logger.error(errorMsg, e);
            loggingService.logEvent(effectiveUsername, "FILE_DOWNLOAD_FAILURE", errorMsg + ": " + e.getMessage(), LogEntry.Status.FAILURE);
            throw new ProcessingException(errorMsg, e);
        }
    }

    public void deleteFile(String fileId, String username) throws ProcessingException {
        String effectiveUsername = normalizeUsername(username);
        try {
            List<ChunkMetadata> chunks = chunkMetadataRepository.findByFileIdOrderByChunkIndexAsc(fileId);
            for (ChunkMetadata chunk : chunks) {
                chunkStorageService.deleteChunk(chunk.getFileServerName(), fileId, chunk.getChunkIndex());
            }
            chunkMetadataRepository.deleteByFileId(fileId);
            fileMetadataRepository.deleteByFileId(fileId);
            loggingService.logEvent(effectiveUsername, "FILE_DELETE_COMPLETE", "File ID " + fileId + " deleted successfully.", LogEntry.Status.SUCCESS);
        } catch (SQLException | ProcessingException e) {
            String errorMsg = "Failed to delete file ID " + fileId;
            logger.error(errorMsg, e);
            loggingService.logEvent(effectiveUsername, "FILE_DELETE_FAILURE", errorMsg + ": " + e.getMessage(), LogEntry.Status.FAILURE);
            throw new ProcessingException(errorMsg, e);
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "admin";
        }
        return username.trim();
    }

    private String normalizeFileId(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return fileId.trim();
    }
}
