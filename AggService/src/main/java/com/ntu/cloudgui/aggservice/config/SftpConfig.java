package com.ntu.cloudgui.aggservice.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpConfig {
    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String remoteDir = "/data/chunks";
    private int connectionTimeout = 30000;
    private List<String> serverHosts;
}
