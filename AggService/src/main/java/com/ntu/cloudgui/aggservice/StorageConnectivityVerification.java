package com.ntu.cloudgui.aggservice;

import com.jcraft.jsch.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class StorageConnectivityVerification {

    public static void main(String[] args) {
        String storageNodesEnv = System.getenv("STORAGE_NODES");
        if (storageNodesEnv == null || storageNodesEnv.isEmpty()) {
            System.err.println("STORAGE_NODES environment variable not set.");
            System.exit(1);
        }

        List<String> storageNodes = Arrays.asList(storageNodesEnv.split(","));
        String sftpUser = System.getenv("SFTP_USER");
        String sftpPass = System.getenv("SFTP_PASS");
        int sftpPort = resolveSftpPort();

        if (sftpUser == null || sftpPass == null) {
            System.err.println("SFTP_USER or SFTP_PASS environment variables not set.");
            System.exit(1);
        }

        for (String host : storageNodes) {
            System.out.println("Testing connection to " + host + "...");
            JSch jsch = new JSch();
            Session session = null;
            ChannelSftp channelSftp = null;
            try {
                session = jsch.getSession(sftpUser, host, sftpPort);
                session.setPassword(sftpPass);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();
                System.out.println("Session connected to " + host);

                channelSftp = (ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                System.out.println("SFTP channel connected to " + host);

                // Create a test file and upload it
                String testFileContent = "This is a test file.";
                InputStream inputStream = new ByteArrayInputStream(testFileContent.getBytes());
                String remotePath = "/config/test-connectivity.txt"; // Use /config as per volume mount
                channelSftp.put(inputStream, remotePath);
                System.out.println("Successfully uploaded test file to " + host);

                // Clean up the test file
                channelSftp.rm(remotePath);
                System.out.println("Successfully deleted test file from " + host);

                System.out.println("SUCCESS: Connectivity to " + host + " verified.");

            } catch (JSchException | SftpException e) {
                System.err.println("ERROR: Failed to connect or transfer to " + host);
                e.printStackTrace();
            } finally {
                if (channelSftp != null) {
                    channelSftp.disconnect();
                }
                if (session != null) {
                    session.disconnect();
                }
                System.out.println("--------------------");
            }
        }
    }

    private static int resolveSftpPort() {
        String rawPort = System.getenv("SFTP_PORT");
        if (rawPort == null || rawPort.isBlank()) {
            return 22;
        }
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException e) {
            System.err.println("Invalid SFTP_PORT '" + rawPort + "', falling back to 22");
            return 22;
        }
    }
}
