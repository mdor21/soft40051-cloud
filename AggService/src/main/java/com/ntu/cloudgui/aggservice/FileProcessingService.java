package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private final EncryptionService encryptionService;
    private final ChunkStorageService chunkStorageService;
    private final CrcValidationService crcValidationService;
    private final FileMetadataRepository fileMetadataRepository;
    private final ChunkMetadataRepository chunkMetadataRepository;
    private final int chunkSize;

    public FileProcessingService(Configuration config, EncryptionService encryptionService, ChunkStorageService chunkStorageService, CrcValidationService crcValidationService, FileMetadataRepository fileMetadataRepository, ChunkMetadataRepository chunkMetadataRepository) {
        this.chunkSize = config.getChunkSize();
        this.encryptionService = encryptionService;
        this.chunkStorageService = chunkStorageService;
        this.crcValidationService = crcValidationService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.chunkMetadataRepository = chunkMetadataRepository;
    }

    public long processAndStoreFile(String filename, byte[] fileData, String username) throws ProcessingException {
        try {
            // 1. Encrypt the entire file
            byte[] encryptedData = encryptionService.encrypt(fileData);
            logger.debug("File encrypted successfully. Original size: {}, Encrypted size: {}", fileData.length, encryptedData.length);

            // 2. Save initial file metadata
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setFilename(filename);
            fileMetadata.setFileSize(fileData.length);
            fileMetadata.setUsername(username);
            fileMetadataRepository.save(fileMetadata);
            long fileId = fileMetadata.getId();
            logger.info("Saved initial metadata for file '{}' with ID: {}", filename, fileId);

            // 3. Chunk the encrypted data and process each chunk
            int chunkIndex = 0;
            for (int offset = 0; offset < encryptedData.length; offset += chunkSize) {
                int length = Math.min(chunkSize, encryptedData.length - offset);
                byte[] chunkBytes = Arrays.copyOfRange(encryptedData, offset, offset + length);

                // a. Compute CRC32 for the chunk
                long crc32 = crcValidationService.calculateCrc32(chunkBytes);

                // b. Store the chunk on a file server
                String server = chunkStorageService.storeChunk(chunkBytes, fileId, chunkIndex);
                logger.debug("Stored chunk {} for file ID {} on server {}", chunkIndex, fileId, server);

                // c. Persist chunk metadata
                ChunkMetadata chunkMetadata = new ChunkMetadata();
                chunkMetadata.setFileId(fileId);
                chunkMetadata.setChunkIndex(chunkIndex);
                chunkMetadata.setCrc32(crc32);
                chunkMetadata.setFileServerName(server);
                chunkMetadata.setChunkPath(String.format("/data/%d/chunk_%d.enc", fileId, chunkIndex));
                chunkMetadata.setChunkSize(length);
                chunkMetadataRepository.save(chunkMetadata);

                chunkIndex++;
            }
            logger.info("Successfully processed and stored {} chunks for file ID {}", chunkIndex, fileId);
            return fileId;

        } catch (SQLException e) {
            logger.error("Database error during file processing for {}", filename, e);
            // Consider implementing rollback logic here
            throw new ProcessingException("Database failure while processing file", e);
        }
    }

    public byte[] retrieveAndReassembleFile(long fileId) throws ProcessingException {
        try {
            // 1. Retrieve chunk metadata
            List<ChunkMetadata> chunkMetadatas = chunkMetadataRepository.findByFileIdOrderByChunkIndexAsc(fileId);
            if (chunkMetadatas.isEmpty()) {
                throw new ProcessingException("File not found or has no associated chunks for ID: " + fileId);
            }
            logger.debug("Found {} chunks for file ID {}", chunkMetadatas.size(), fileId);

            // 2. Retrieve, validate, and concatenate chunks
            ByteArrayOutputStream reassembledEncryptedStream = new ByteArrayOutputStream();
            for (ChunkMetadata chunk : chunkMetadatas) {
                // a. Retrieve chunk from file server
                byte[] chunkBytes = chunkStorageService.retrieveChunk(chunk.getFileServerName(), fileId, chunk.getChunkIndex());

                // b. Validate CRC32
                if (!crcValidationService.validateCrc32(chunkBytes, chunk.getCrc32())) {
                    throw new ProcessingException("CRC32 check failed for chunk " + chunk.getChunkIndex() + " of file ID " + fileId);
                }
                logger.debug("CRC32 validation passed for chunk {}", chunk.getChunkIndex());

                // c. Append to stream
                reassembledEncryptedStream.write(chunkBytes);
            }

            // 3. Decrypt the reassembled data
            byte[] reassembledEncryptedData = reassembledEncryptedStream.toByteArray();
            byte[] decryptedData = encryptionService.decrypt(reassembledEncryptedData);
            logger.info("Successfully retrieved and decrypted file ID {}", fileId);

            return decryptedData;

        } catch (SQLException e) {
            logger.error("Database error during file retrieval for ID: {}", fileId, e);
            throw new ProcessingException("Database failure while retrieving file", e);
        } catch (IOException e) {
            logger.error("IO error during file reassembly for ID: {}", fileId, e);
            throw new ProcessingException("Failed to reassemble file chunks", e);
        }
    }
}
