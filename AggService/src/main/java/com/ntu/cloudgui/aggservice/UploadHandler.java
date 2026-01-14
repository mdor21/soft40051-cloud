package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UploadHandler {

    private static final Logger logger = LoggerFactory.getLogger(UploadHandler.class);
    private final FileProcessingService fileProcessingService;

    public UploadHandler(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    public void handle(InputStream in, OutputStream out, Map<String, String> headers) throws IOException {
        // 1. Validate headers
        String username = headers.get("USERNAME");
        String filename = headers.get("FILENAME");
        String contentLengthStr = headers.get("CONTENT_LENGTH");
        String fileId = headers.get("FILE_ID");

        if (username == null || filename == null || contentLengthStr == null) {
            sendError(out, "ERROR BAD_REQUEST Missing required headers");
            return;
        }

        long contentLength;
        try {
            contentLength = Long.parseLong(contentLengthStr);
            if (contentLength <= 0) {
                sendError(out, "ERROR BAD_REQUEST Invalid CONTENT_LENGTH");
                return;
            }
        } catch (NumberFormatException e) {
            sendError(out, "ERROR BAD_REQUEST Malformed CONTENT_LENGTH");
            return;
        }

        // 2. Read file data
        byte[] fileData = new byte[(int) contentLength];
        int totalBytesRead = 0;
        int bytesRead;
        while (totalBytesRead < contentLength && (bytesRead = in.read(fileData, totalBytesRead, (int) (contentLength - totalBytesRead))) != -1) {
            totalBytesRead += bytesRead;
        }

        if (totalBytesRead != contentLength) {
            sendError(out, "ERROR BAD_REQUEST Incomplete file data received");
            return;
        }

        try {
            String storedFileId = fileProcessingService.processAndStoreFile(filename, fileData, username, fileId);
            sendSuccess(out, storedFileId);
        } catch (ProcessingException e) {
            logger.error("Error processing file '{}'", filename, e);
            sendError(out, "ERROR INTERNAL_SERVER_ERROR " + e.getMessage());
        }
    }

    private void sendSuccess(OutputStream out, String fileId) throws IOException {
        String response = "OK FILE_ID: " + fileId + "\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
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
