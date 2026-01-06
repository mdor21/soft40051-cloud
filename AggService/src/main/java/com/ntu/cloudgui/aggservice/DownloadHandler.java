package com.ntu.cloudgui.aggservice;

import com.ntu.cloudgui.aggservice.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class DownloadHandler {

    private static final Logger logger = LoggerFactory.getLogger(DownloadHandler.class);

    private final FileProcessingService fileProcessingService;

    public DownloadHandler(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    public void handle(OutputStream out, PrintWriter writer, Map<String, String> headers) throws IOException {
        String fileId = headers.get("FILE_ID");

        if (fileId == null) {
            writer.println("ERROR BAD_REQUEST Missing FILE_ID header");
            return;
        }

        try {
            FileMetadata fileMetadata = fileProcessingService.getFile(fileId);
            if (fileMetadata == null) {
                writer.println("ERROR FILE_NOT_FOUND");
                return;
            }

            byte[] fileData = fileProcessingService.processDownload(fileId, fileMetadata.getEncryptionAlgo());

            writer.println("OK CONTENT_LENGTH: " + fileData.length);
            writer.println("FILENAME: " + fileMetadata.getOriginalName());
            writer.println("END_HEADERS");
            writer.flush();

            out.write(fileData);
            out.flush();
        } catch (Exception e) {
            logger.error("Error processing download", e);
            writer.println("ERROR INTERNAL_SERVER_ERROR " + e.getMessage());
        }
    }
}
