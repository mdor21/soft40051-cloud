package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.config.SshConfig;

import java.io.InputStream;

/**
 * Service responsible for transferring chunks to/from File Server containers.
 *
 * This stub version does not yet use real SSH/SFTP; it just returns
 * dummy paths/streams so the coursework compiles and runs.
 */
public class ChunkStorageService {

    private final SshConfig sshConfig;

    public ChunkStorageService(SshConfig sshConfig) {
        this.sshConfig = sshConfig;
    }

    public String storeChunk(InputStream chunkData, int chunkIndex) {
        // TODO: replace with real SFTP upload using sshConfig.
        return "/chunks/chunk-" + chunkIndex;
    }

    public InputStream fetchChunk(String remotePath) {
        // TODO: replace with real SFTP download using sshConfig.
        return InputStream.nullInputStream();
    }
}
