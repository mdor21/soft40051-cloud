package com.ntu.cloudgui.aggservice;

import com.ntu.cloudgui.aggservice.service.FileProcessingService;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final FileProcessingService fileProcessingService;

    public ClientHandler(Socket clientSocket, FileProcessingService fileProcessingService) {
        this.clientSocket = clientSocket;
        this.fileProcessingService = fileProcessingService;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String command = in.readLine();
            if (command == null) {
                return;
            }

            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = in.readLine()) != null && !"END_HEADERS".equals(line)) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    headers.put(parts[0], parts[1]);
                }
            }

            if ("UPLOAD_FILE".equals(command)) {
                handleUpload(headers, in, out);
            } else if ("DOWNLOAD_FILE".equals(command)) {
                handleDownload(headers, out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUpload(Map<String, String> headers, BufferedReader in, PrintWriter out) throws IOException {
        String filename = headers.get("FILENAME");
        int contentLength = Integer.parseInt(headers.get("CONTENT_LENGTH"));
        byte[] buffer = new byte[contentLength];
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            bytesRead += clientSocket.getInputStream().read(buffer, bytesRead, contentLength - bytesRead);
        }

        File tempFile = File.createTempFile("upload_", "_" + filename);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(buffer);
        }

        try {
            String fileId = fileProcessingService.processUpload(tempFile, "AES");
            out.println("OK FILE_ID: " + fileId);
        } catch (Exception e) {
            out.println("ERROR " + e.getMessage());
        } finally {
            tempFile.delete();
        }
    }

    private void handleDownload(Map<String, String> headers, PrintWriter out) throws IOException {
        String fileId = headers.get("FILE_ID");
        try {
            byte[] fileData = fileProcessingService.processDownload(fileId, "AES");
            out.println("OK CONTENT_LENGTH: " + fileData.length);
            out.println("FILENAME: " + fileProcessingService.getFile(fileId).getOriginalName());
            out.println("END_HEADERS");
            clientSocket.getOutputStream().write(fileData);
        } catch (Exception e) {
            out.println("ERROR " + e.getMessage());
        }
    }
}
