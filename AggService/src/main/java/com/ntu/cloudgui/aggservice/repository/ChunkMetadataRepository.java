package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import java.util.List;

/**
 * DAO / Repository for ChunkMetadata.
 *
 * Responsibilities:
 * - Persist per‑chunk metadata during upload.
 * - Fetch all chunks for a specific file in correct order.
 */
public class ChunkMetadataRepository {

    // TODO: Hold a reference to DatabaseConfig / DataSource.

    public void save(ChunkMetadata chunkMetadata) {
        // TODO: Insert row into chunk metadata table.
    }

    public List<ChunkMetadata> findByFileIdOrderByChunkIndex(String fileId) {
        // TODO: Select all rows for fileId and order by chunk index.
        return null;
    }
}
