package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import com.ntu.cloudgui.aggservice.model.FileMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service for all interactions with metadata.
 *
 * For coursework, this uses an in-memory list so the pipeline works
 * without a real MySQL implementation. You can later replace the
 * internals with FileMetadataRepository and ChunkMetadataRepository.
 */
public class MetadataService {

    private final List<FileMetadata> files = new ArrayList<>();
    private final List<ChunkMetadata> chunks = new ArrayList<>();

    /**
     * Persist global information about an uploaded file.
     *
     * @param metadata file-level metadata
     * @return saved FileMetadata
     */
    public FileMetadata saveFileMetadata(FileMetadata metadata) {
        files.add(metadata);
        return metadata;
    }

    /**
     * Persist per-chunk metadata (location, CRC32, size, etc.).
     */
    public void saveChunkMetadata(ChunkMetadata chunkMetadata) {
        chunks.add(chunkMetadata);
    }

    /**
     * Retrieve all chunk metadata records for a given file,
     * sorted by chunkIndex.
     */
    public List<ChunkMetadata> getChunksForFile(String fileId) {
        List<ChunkMetadata> result = new ArrayList<>();
        for (ChunkMetadata cm : chunks) {
            if (fileId.equals(cm.getFileId())) {
                result.add(cm);
            }
        }
        result.sort(Comparator.comparingInt(ChunkMetadata::getChunkIndex));
        return result;
    }

    /**
     * Fetch main file metadata.
     *
     * @param fileId identifier
     * @return FileMetadata or null if not found
     */
    public FileMetadata getFileMetadata(String fileId) {
        for (FileMetadata fm : files) {
            if (fileId.equals(fm.getFileId())) {
                return fm;
            }
        }
        return null;
    }
}
