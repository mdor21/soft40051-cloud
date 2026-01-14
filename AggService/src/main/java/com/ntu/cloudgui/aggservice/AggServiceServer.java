package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class AggServiceServer {

    private static final Logger logger = LoggerFactory.getLogger(AggServiceServer.class);
    private final int port;
    private final ExecutorService executorService;
    private final DatabaseManager databaseManager;
    private final FileProcessingService fileProcessingService;
    private final FileMetadataRepository fileMetadataRepository;
    private LbLogServer lbLogServer;
    private AggApiServer apiServer;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public AggServiceServer(Configuration config) {
        this.port = config.getAggServicePort();
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());

        // Initialize core components
        this.databaseManager = new DatabaseManager(config);

        if (config.isResetSchema()) {
            // Reset database schema before initializing the connection pool
            try (java.sql.Connection schemaConnection = databaseManager.createDirectConnection()) {
                SchemaManager schemaManager = new SchemaManager();
                schemaManager.resetDatabaseSchema(schemaConnection);
            } catch (java.sql.SQLException e) {
                logger.error("Failed to apply database schema. Shutting down.", e);
                System.exit(1); // Critical failure
            }
        } else {
            logger.info("RESET_SCHEMA is false; skipping schema reset.");
        }

        // Now that the schema is ready, initialize the connection pool
        initializePoolWithRetry(config);

        try {
            lbLogServer = new LbLogServer(databaseManager, config.getLbLogPort());
            lbLogServer.start();
            logger.info("LB log ingestion server started on port {}", config.getLbLogPort());
        } catch (Exception e) {
            logger.warn("Failed to start LB log ingestion server: {}", e.getMessage());
        }

        this.fileMetadataRepository = new FileMetadataRepository(databaseManager);
        ChunkMetadataRepository chunkMetadataRepository = new ChunkMetadataRepository(databaseManager);
        LogEntryRepository logEntryRepository = new LogEntryRepository(databaseManager);

        DatabaseLoggingService loggingService = new DatabaseLoggingService(logEntryRepository);
        EncryptionService encryptionService = new EncryptionService(config.getEncryptionKey());
        CrcValidationService crcValidationService = new CrcValidationService();
        ChunkStorageService chunkStorageService = new ChunkStorageService(config);

        // Initialize the main service orchestrator
        this.fileProcessingService = new FileProcessingService(
            config,
            encryptionService,
            chunkStorageService,
            crcValidationService,
            fileMetadataRepository,
            chunkMetadataRepository,
            loggingService
        );
    }

    public void start() {
        try {
            apiServer = new AggApiServer(port, executorService, fileProcessingService, fileMetadataRepository);
            apiServer.start();
            logger.info("AggService HTTP API started on port {}", port);
            shutdownLatch.await();
        } catch (IOException e) {
            logger.error("Could not start HTTP server on port {}", port, e);
            shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdown();
        }
    }

    private void initializePoolWithRetry(Configuration config) {
        int retries = config.getDbConnectRetries();
        int delayMs = config.getDbConnectDelayMs();
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                databaseManager.initializePool();
                return;
            } catch (RuntimeException e) {
                databaseManager.closePool();
                if (attempt >= retries) {
                    throw e;
                }
                logger.warn("Database not ready (attempt {}/{}). Retrying in {}ms.", attempt, retries, delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    public void shutdown() {
        logger.info("Shutting down AggServiceServer.");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        databaseManager.closePool();
        if (lbLogServer != null) {
            lbLogServer.stop();
        }
        if (apiServer != null) {
            apiServer.stop();
        }
        shutdownLatch.countDown();
        logger.info("AggServiceServer shut down complete.");
    }

    public static void main(String[] args) {
        try {
            Configuration config = new Configuration();
            AggServiceServer server = new AggServiceServer(config);

            // Add a shutdown hook to gracefully close the server
            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

            server.start();
        } catch (Exception e) {
            logger.error("Failed to start AggServiceServer", e);
            System.exit(1);
        }
    }
}
