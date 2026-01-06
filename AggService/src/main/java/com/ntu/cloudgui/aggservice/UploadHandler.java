package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;

public class UploadHandler {

    private static final Logger logger = LoggerFactory.getLogger(UploadHandler.class);

    private final FileProcessingService fileProcessingService;

    public UploadHandler(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    public void handle(InputStream in, PrintWriter out, Map<String, String> headers) throws IOException {
        String username = headers.get("USERNAME");
        String filename = headers.get("FILENAME");
        long contentLength;
        try {
            contentLength = Long.parseLong(headers.get("CONTENT_LENGTH"));
        } catch (NumberFormatException e) {
            out.println("ERROR BAD_REQUEST Invalid CONTENT_LENGTH");
            return;
        }

        if (username == null || filename == null || contentLength <= 0) {
            out.println("ERROR BAD_REQUEST Missing required headers");
            return;
        }

        File tempFile = File.createTempFile("upload-", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            while (totalBytesRead < contentLength && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, contentLength - totalBytesRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            if (totalBytesRead != contentLength) {
                out.println("ERROR BAD_REQUEST Incomplete file data");
                return;
            }

            String fileId = fileProcessingService.processUpload(tempFile, "AES");
            out.println("OK FILE_ID: " + fileId);
        } catch (Exception e) {
            logger.error("Error processing upload", e);
            out.println("ERROR INTERNAL_SERVER_ERROR " + e.getMessage());
        } finally {
            tempFile.delete();
        }
    }
}
