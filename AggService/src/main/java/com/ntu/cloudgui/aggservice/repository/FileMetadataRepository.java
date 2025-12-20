package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.model.FileMetadata;

/**
 * DAO / Repository for FileMetadata.
 *
 * Responsibilities:
 * - Execute SQL statements against MySQL (insert, update, select).
 * - Map ResultSet rows into FileMetadata objects.
 *
 * Implementation choices:
 * - Use plain JDBC.
 * - Or use a framework (e.g., JPA) if available in your project.
 */
public class FileMetadataRepository {

    // TODO: Hold a reference to DatabaseConfig / DataSource.

    public FileMetadata save(FileMetadata fileMetadata) {
        // TODO: Insert record and return with generated fileId.
        return null;
    }

    public FileMetadata findByFileId(String fileId) {
        // TODO: Select by fileId.
        return null;
    }
}
