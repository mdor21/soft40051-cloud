package com.ntu.cloudgui.aggservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chunk_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String chunkId;
    private String fileId;
    private int chunkIndex;
    private String serverHost;
    private String remotePath;
    private long originalSize;
    private long encryptedSize;
    private long crc32;
    private LocalDateTime createdAt;

    public ChunkMetadata(String fileId, int chunkIndex, String serverHost, String remotePath, 
                        long originalSize, long encryptedSize, long crc32, LocalDateTime createdAt) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.serverHost = serverHost;
        this.remotePath = remotePath;
        this.originalSize = originalSize;
        this.encryptedSize = encryptedSize;
        this.crc32 = crc32;
        this.createdAt = createdAt;
    }
}
