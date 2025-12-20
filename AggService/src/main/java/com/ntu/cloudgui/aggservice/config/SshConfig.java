package com.ntu.cloudgui.aggservice.config;

/**
 * Configuration for SSH/SFTP connectivity to File Server containers.
 *
 * Values are read from environment variables, with sensible defaults.
 */
public class SshConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public SshConfig(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public static SshConfig fromEnvOrDefaults() {
        String host = System.getenv().getOrDefault("SSH_HOST", "file-server-1");
        int port = Integer.parseInt(System.getenv().getOrDefault("SSH_PORT", "22"));
        String user = System.getenv().getOrDefault("SSH_USER", "root");
        String pass = System.getenv().getOrDefault("SSH_PASSWORD", "root");
        return new SshConfig(host, port, user, pass);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
