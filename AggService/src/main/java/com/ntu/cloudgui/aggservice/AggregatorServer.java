package com.ntu.cloudgui.aggservice;

import com.ntu.cloudgui.aggservice.repository.ChunkMetadataDao;
import com.ntu.cloudgui.aggservice.repository.FileMetadataDao;
import com.ntu.cloudgui.aggservice.service.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AggregatorServer {

    private final int port;
    private final ExecutorService executorService;
    private final FileProcessingService fileProcessingService;

    public AggregatorServer(int port, FileProcessingService fileProcessingService) {
        this.port = port;
        this.fileProcessingService = fileProcessingService;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregator server started on port " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket, fileProcessingService));
            }
        } catch (IOException e) {
            System.err.println("Could not start aggregator server on port " + port);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws SQLException {
        Configuration config = new Configuration();
        Connection connection = DriverManager.getConnection(
                "jdbc:mysql://" + config.getDbHost() + ":" + config.getDbPort() + "/" + config.getDbName(),
                config.getDbUser(),
                config.getDbPassword()
        );

        FileMetadataDao fileMetadataDao = new FileMetadataDao(connection);
        ChunkMetadataDao chunkMetadataDao = new ChunkMetadataDao(connection);
        DatabaseLoggingService databaseLoggingService = new DatabaseLoggingService(connection);
        EncryptionService encryptionService = new EncryptionService();
        CrcValidationService crcValidationService = new CrcValidationService();
        ChunkStorageService chunkStorageService = new ChunkStorageService(config.getFileServers(), "user", "password");
        FileProcessingService fileProcessingService = new FileProcessingService(
                fileMetadataDao,
                chunkMetadataDao,
                encryptionService,
                chunkStorageService,
                crcValidationService,
                databaseLoggingService,
                config.getSemaphorePermits()
        );

        new AggregatorServer(config.getAggregatorPort(), fileProcessingService).start();
    }
}
