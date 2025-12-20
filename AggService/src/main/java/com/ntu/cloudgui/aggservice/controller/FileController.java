package com.ntu.cloudgui.aggservice.controller;

import java.io.InputStream;

/**
 * REST controller (or HTTP handler) exposing upload and download endpoints.
 *
 * Endpoints (to be implemented):
 * - POST /files/upload : accepts a file stream and metadata, forwards to service.
 * - GET  /files/{fileId}/download : streams reconstructed file to the client.
 *
 * This class must:
 * - Validate incoming parameters.
 * - Translate service-layer exceptions into HTTP status codes.
 * - Never contain business logic like encryption or chunking.
 */
public class FileController {

    // TODO: Inject FileProcessingService once created.

    /**
     * Handles file upload requests.
     * @param inputStream the raw file data from client.
     * @param originalFileName logical name of the uploaded file.
     */
    public void uploadFile(InputStream inputStream, String originalFileName) {
        // TODO: Delegate to FileProcessingService.encryptAndChunk(...)
        // TODO: Handle and log any FileProcessingException / StorageException.
    }

    /**
     * Handles file download requests.
     * @param fileId identifier of the stored file.
     * @return an InputStream that will be written to HTTP response.
     */
    public InputStream downloadFile(String fileId) {
        // TODO: Delegate to FileProcessingService.reconstructAndDecrypt(...)
        return null;
    }
}
