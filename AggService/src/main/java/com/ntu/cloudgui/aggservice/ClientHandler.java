package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket clientSocket;
    private final FileProcessingService fileProcessingService;
    private final FileMetadataRepository fileMetadataRepository;

    public ClientHandler(Socket clientSocket, FileProcessingService fileProcessingService, FileMetadataRepository fileMetadataRepository) {
        this.clientSocket = clientSocket;
        this.fileProcessingService = fileProcessingService;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            OutputStream out = clientSocket.getOutputStream()
        ) {
            String commandLine = reader.readLine();
            if (commandLine == null || commandLine.trim().isEmpty()) {
                sendError(out, "ERROR BAD_REQUEST Empty command");
                return;
            }

            String[] commandParts = commandLine.trim().split("\\s+");
            String command = commandParts[0].toUpperCase();

            Map<String, String> headers = readHeaders(reader);

            switch (command) {
                case "UPLOAD_FILE":
                    new UploadHandler(fileProcessingService).handle(clientSocket.getInputStream(), out, headers);
                    break;
                case "DOWNLOAD_FILE":
                    new DownloadHandler(fileProcessingService, fileMetadataRepository).handle(out, headers);
                    break;
                default:
                    logger.warn("Unknown command received: {}", command);
                    sendError(out, "ERROR UNKNOWN_COMMAND");
                    break;
            }

        } catch (IOException e) {
            logger.error("I/O error handling client request from {}", clientSocket.getRemoteSocketAddress(), e);
        } catch (Exception e) {
            logger.error("Unexpected error handling client request from {}", clientSocket.getRemoteSocketAddress(), e);
            try {
                if (!clientSocket.isClosed()) {
                    sendError(clientSocket.getOutputStream(), "ERROR INTERNAL_SERVER_ERROR " + e.getMessage());
                }
            } catch (IOException ex) {
                logger.error("Failed to send error response", ex);
            }
        } finally {
            try {
                clientSocket.close();
                logger.info("Closed connection from {}", clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                logger.error("Error closing client socket", e);
            }
        }
    }

    private Map<String, String> readHeaders(BufferedReader reader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END_HEADERS")) {
            if (line.isEmpty()) continue;
            int separatorIndex = line.indexOf(':');
            if (separatorIndex > 0) {
                String key = line.substring(0, separatorIndex).trim().toUpperCase();
                String value = line.substring(separatorIndex + 1).trim();
                headers.put(key, value);
            }
        }
        return headers;
    }

    private void sendError(OutputStream out, String errorMessage) {
        try {
            out.write((errorMessage + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            logger.error("Failed to send error response to client", e);
        }
    }
}
