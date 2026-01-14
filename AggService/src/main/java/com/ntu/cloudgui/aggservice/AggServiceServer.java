package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class AggServiceServer {

    private static final Logger logger = LoggerFactory.getLogger(AggServiceServer.class);
    private final int port;
    private final ExecutorService executorService;
    private final DatabaseManager databaseManager;
    private final FileProcessingService fileProcessingService;
    private final FileMetadataRepository fileMetadataRepository;

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
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("AggServiceServer started on port {}", port);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Accepted connection from {}", clientSocket.getRemoteSocketAddress());
                    // Pass the services to the handler
                    ClientHandler handler = new ClientHandler(clientSocket, fileProcessingService, fileMetadataRepository);
                    executorService.submit(handler);
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        logger.info("Server socket interrupted, shutting down.");
                        break;
                    }
                    logger.error("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            logger.error("Could not start server on port {}", port, e);
        } finally {
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
