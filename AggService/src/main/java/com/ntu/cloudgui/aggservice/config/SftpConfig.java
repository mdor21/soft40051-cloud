package com.ntu.cloudgui.aggservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpConfig {

    private String host;
    private int port;
    private String username;
    private String password;
    private int connectionTimeout = 30000;
    private String[] serverHosts;

    // Getters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public String[] getServerHosts() { return serverHosts; }
    public void setServerHosts(String[] serverHosts) { this.serverHosts = serverHosts; }
}
