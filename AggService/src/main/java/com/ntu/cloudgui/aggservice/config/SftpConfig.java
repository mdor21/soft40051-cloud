package com.ntu.cloudgui.aggservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sftp")
public class SftpConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String[] serverHosts;
    private int connectionTimeout = 30000;
    private String baseDirectory = "/uploads";
    private int maxPoolSize = 10;
    private int minIdleConnections = 2;
}
