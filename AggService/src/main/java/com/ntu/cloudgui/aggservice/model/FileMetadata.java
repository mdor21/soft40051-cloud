package com.ntu.cloudgui.aggservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String fileId;
    private String originalName;
    private int totalChunks;
    private long sizeBytes;
    private String encryptionAlgo;
    private LocalDateTime uploadedAt;

    public FileMetadata(String originalName, String encryptionAlgo, int totalChunks, long sizeBytes) {
        this.originalName = originalName;
        this.encryptionAlgo = encryptionAlgo;
        this.totalChunks = totalChunks;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return fileId != null && originalName != null && totalChunks > 0 && sizeBytes > 0;
    }
}
