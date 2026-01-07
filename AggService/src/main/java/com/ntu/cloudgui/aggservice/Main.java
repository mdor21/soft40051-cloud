package com.ntu.cloudgui.aggservice;

public class Main {
    public static void main(String[] args) {
        Configuration config = new Configuration();
        DatabaseManager dbManager = new DatabaseManager(config);
        FileMetadataRepository fileMetadataRepository = new FileMetadataRepository(dbManager);
        ChunkMetadataRepository chunkMetadataRepository = new ChunkMetadataRepository(dbManager);
        EncryptionService encryptionService = new EncryptionService();
        CrcValidationService crcValidationService = new CrcValidationService();
        SftpConnectionPool sftpConnectionPool = new SftpConnectionPool(config);
        ChunkStorageService chunkStorageService = new ChunkStorageService(config, sftpConnectionPool);
        FileProcessingService fileProcessingService = new FileProcessingService(
                fileMetadataRepository,
                chunkMetadataRepository,
                encryptionService,
                chunkStorageService,
                crcValidationService,
                10,
                config.getChunkSize()
        );

        AggServiceServer server = new AggServiceServer(config, fileProcessingService);
        server.start();
    }
}
