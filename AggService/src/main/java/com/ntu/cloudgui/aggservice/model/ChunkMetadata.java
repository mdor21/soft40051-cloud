package com.ntu.cloudgui.aggservice.model;

/**
 * Metadata for a single encrypted chunk of a file.
 *
 * Maps to a table in the remote MySQL database.
 */
public class ChunkMetadata {

    private Long id;              // optional DB primary key
    private String fileId;        // link to FileMetadata.fileId
    private int chunkIndex;       // order of this chunk within the file
    private String serverHost;    // file server hostname or identifier
    private String remotePath;    // path or key on the file server
    private long crc32Checksum;   // CRC32 checksum for integrity
    private long sizeBytes;       // size of this chunk in bytes

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public long getCrc32Checksum() {
        return crc32Checksum;
    }

    public void setCrc32Checksum(long crc32Checksum) {
        this.crc32Checksum = crc32Checksum;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    @Override
    public String toString() {
        return "ChunkMetadata{" +
                "fileId='" + fileId + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", serverHost='" + serverHost + '\'' +
                ", remotePath='" + remotePath + '\'' +
                ", sizeBytes=" + sizeBytes +
                '}';
    }
}
