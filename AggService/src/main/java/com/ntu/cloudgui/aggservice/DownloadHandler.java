package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.sql.SQLException;

public class DownloadHandler {

    private static final Logger logger = LoggerFactory.getLogger(DownloadHandler.class);
    private final FileProcessingService fileProcessingService;
    private final FileMetadataRepository fileMetadataRepository;

    public DownloadHandler(FileProcessingService fileProcessingService, FileMetadataRepository fileMetadataRepository) {
        this.fileProcessingService = fileProcessingService;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    public void handle(OutputStream out, Map<String, String> headers) throws IOException {
        String fileIdStr = headers.get("FILE_ID");
        if (fileIdStr == null || fileIdStr.trim().isEmpty()) {
            sendError(out, "ERROR BAD_REQUEST Missing FILE_ID header");
            return;
        }

        long fileId;
        try {
            fileId = Long.parseLong(fileIdStr);
        } catch (NumberFormatException e) {
            sendError(out, "ERROR BAD_REQUEST Invalid FILE_ID format");
            return;
        }

        try {
            FileMetadata fileMetadata = fileMetadataRepository.findById(fileId);
            if (fileMetadata == null) {
                sendError(out, "ERROR FILE_NOT_FOUND");
                return;
            }

            byte[] fileData = fileProcessingService.retrieveAndReassembleFile(fileId, fileMetadata.getUsername());
            sendSuccess(out, fileMetadata.getFilename(), fileData);

        } catch (ProcessingException e) {
            logger.error("Error processing download for file ID: {}", fileId, e);
            if (e.getMessage().contains("File not found")) {
                sendError(out, "ERROR FILE_NOT_FOUND");
            } else {
                sendError(out, "ERROR INTERNAL_SERVER_ERROR " + e.getMessage());
            }
        } catch (SQLException e) {
            logger.error("Database error during download for file ID: {}", fileId, e);
            sendError(out, "ERROR DATABASE_ERROR " + e.getMessage());
        }
    }

    private void sendSuccess(OutputStream out, String filename, byte[] fileData) throws IOException {
        String headers = "OK CONTENT_LENGTH: " + fileData.length + "\n" +
                         "FILENAME: " + filename + "\n" +
                         "END_HEADERS\n";
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(fileData);
        out.flush();
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
