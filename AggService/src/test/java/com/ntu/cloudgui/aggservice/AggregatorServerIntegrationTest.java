package com.ntu.cloudgui.aggservice;

import com.ntu.cloudgui.aggservice.service.FileProcessingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AggregatorServerIntegrationTest {

    private Thread serverThread;
    private AggregatorServer server;
    private FileProcessingService mockFileProcessingService;

    @BeforeEach
    public void setUp() throws Exception {
        mockFileProcessingService = Mockito.mock(FileProcessingService.class);
        when(mockFileProcessingService.processUpload(any(), any())).thenReturn("mock-file-id");
        server = new AggregatorServer(9999, mockFileProcessingService);
        serverThread = new Thread(() -> server.start());
        serverThread.start();
        // Give the server a moment to start
        Thread.sleep(100);
    }

    @AfterEach
    public void tearDown() {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    @Test
    public void testUploadFile() throws Exception {
        try (Socket clientSocket = new Socket("localhost", 9999);
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            // Send command and headers
            out.println("UPLOAD_FILE");
            out.println("USERNAME: testuser");
            out.println("FILENAME: test.txt");
            out.println("CONTENT_LENGTH: 4");
            out.println("END_HEADERS");

            // Send file data
            clientSocket.getOutputStream().write("test".getBytes());
            clientSocket.getOutputStream().flush();

            // Read response
            String response = in.readLine();
            assertEquals("OK FILE_ID: mock-file-id", response);
        }
    }

    @Test
    public void testDownloadFile() throws Exception {
        byte[] fileData = "test-download".getBytes();
        when(mockFileProcessingService.processDownload(any(), any())).thenReturn(fileData);
        when(mockFileProcessingService.getFile(any())).thenReturn(new com.ntu.cloudgui.aggservice.model.FileMetadata("mock-file-id", "download.txt", 1, fileData.length, "AES", java.time.LocalDateTime.now()));

        try (Socket clientSocket = new Socket("localhost", 9999);
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            // Send command and headers
            out.println("DOWNLOAD_FILE");
            out.println("FILE_ID: mock-file-id");
            out.println("END_HEADERS");

            // Read response
            assertEquals("OK CONTENT_LENGTH: " + fileData.length, in.readLine());
            assertEquals("FILENAME: download.txt", in.readLine());
            assertEquals("END_HEADERS", in.readLine());

            // Read file data
            byte[] buffer = new byte[fileData.length];
            int bytesRead = clientSocket.getInputStream().read(buffer);
            assertEquals(fileData.length, bytesRead);
            assertEquals(new String(fileData), new String(buffer));
        }
    }

    @Test
    public void testUploadError() throws Exception {
        when(mockFileProcessingService.processUpload(any(), any())).thenThrow(new RuntimeException("Test error"));

        try (Socket clientSocket = new Socket("localhost", 9999);
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            // Send command and headers
            out.println("UPLOAD_FILE");
            out.println("USERNAME: testuser");
            out.println("FILENAME: test.txt");
            out.println("CONTENT_LENGTH: 4");
            out.println("END_HEADERS");

            // Send file data
            clientSocket.getOutputStream().write("test".getBytes());
            clientSocket.getOutputStream().flush();

            // Read response
            String response = in.readLine();
            assertEquals("ERROR Test error", response);
        }
    }
}
