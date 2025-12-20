package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.exception.FileProcessingException;
import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.model.FileMetadata;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Core pipeline: encryption, chunking, CRC validation, reconstruction, decryption.
 *
 * This version provides a clear structure and defers real crypto/chunk I/O
 * to ChunkStorageService and MetadataService.
 */
public class FileProcessingService {

    // Chunk size after encryption (e.g. 1 MB)
    private static final int CHUNK_SIZE_BYTES = 1_000_000;

    private final ChunkStorageService chunkStorageService;
    private final MetadataService metadataService;

    public FileProcessingService(ChunkStorageService chunkStorageService,
                                 MetadataService metadataService) {
        this.chunkStorageService = chunkStorageService;
        this.metadataService = metadataService;
    }

    /**
     * Upload pipeline entry point.
     *
     * @param originalFile     input stream from client
     * @param originalFileName logical file name for metadata
     * @return generated fileId used to retrieve the file later
     */
    public String encryptAndChunk(InputStream originalFile, String originalFileName) {
        String fileId = UUID.randomUUID().toString();

        try {
            // TODO: set real encryption algorithm once implemented
            String encryptionAlgo = "AES/GCM/NoPadding";

            FileMetadata meta = new FileMetadata();
            meta.setFileId(fileId);
            meta.setOriginalName(originalFileName);
            meta.setEncryptionAlgo(encryptionAlgo);

            int chunkIndex = 0;
            long totalSize = 0;

            byte[] buffer = new byte[CHUNK_SIZE_BYTES];
            int read;
            while ((read = originalFile.read(buffer)) != -1) {
                totalSize += read;

                // TODO: encrypt buffer[0..read) before CRC / storage
                byte[] encryptedChunk = new byte[read];
                System.arraycopy(buffer, 0, encryptedChunk, 0, read);

                long crc = crc32(encryptedChunk, 0, read);

                // Store chunk remotely
                InputStream chunkStream = new java.io.ByteArrayInputStream(encryptedChunk);
                String remotePath = chunkStorageService.storeChunk(chunkStream, chunkIndex);

                // Persist chunk metadata
                ChunkMetadata chunkMeta = new ChunkMetadata();
                chunkMeta.setFileId(fileId);
                chunkMeta.setChunkIndex(chunkIndex);
                chunkMeta.setServerHost("TODO-server");    // fill from SshConfig / routing
                chunkMeta.setRemotePath(remotePath);
                chunkMeta.setCrc32Checksum(crc);
                chunkMeta.setSizeBytes(read);

                metadataService.saveChunkMetadata(chunkMeta);
                chunkIndex++;
            }

            meta.setTotalChunks(chunkIndex);
            meta.setSizeBytes(totalSize);
            metadataService.saveFileMetadata(meta);

            return fileId;

        } catch (Exception e) {
            throw new FileProcessingException("Failed to encrypt and chunk file: " + e.getMessage(), e);
        }
    }

    /**
     * Download pipeline entry point.
     *
     * @param fileId            identifier of stored file
     * @param targetOutputStream where decrypted bytes should be written
     */
    public void reconstructAndDecrypt(String fileId, OutputStream targetOutputStream) {
        try {
            FileMetadata meta = metadataService.getFileMetadata(fileId);
            if (meta == null) {
                throw new FileProcessingException("File metadata not found for id=" + fileId);
            }

            List<ChunkMetadata> chunks = metadataService.getChunksForFile(fileId);
            // In a real implementation, ensure chunks are sorted by chunkIndex.

            for (ChunkMetadata chunk : chunks) {
                try (InputStream in = chunkStorageService.fetchChunk(chunk.getRemotePath())) {
                    byte[] buffer = in.readAllBytes();

                    long crc = crc32(buffer, 0, buffer.length);
                    if (crc != chunk.getCrc32Checksum()) {
                        throw new FileProcessingException(
                                "CRC mismatch for chunk " + chunk.getChunkIndex() + " of file " + fileId);
                    }

                    // TODO: decrypt buffer before writing to targetOutputStream
                    targetOutputStream.write(buffer);
                }
            }

            targetOutputStream.flush();

        } catch (Exception e) {
            throw new FileProcessingException("Failed to reconstruct and decrypt file: " + e.getMessage(), e);
        }
    }

    // -------- helper methods --------

    private static long crc32(byte[] data, int off, int len) {
        CRC32 crc = new CRC32();
        crc.update(data, off, len);
        return crc.getValue();
    }
}
