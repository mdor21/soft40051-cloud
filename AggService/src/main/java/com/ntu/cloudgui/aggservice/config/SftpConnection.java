package com.ntu.cloudgui.aggservice.config;

import com.jcraft.jsch.ChannelSftp;
import lombok.Data;

@Data
public class SftpConnection {
    private ChannelSftp sftp;
    private long lastUsed;

    public SftpConnection(ChannelSftp sftp) {
        this.sftp = sftp;
        this.lastUsed = System.currentTimeMillis();
    }

    public void updateLastUsed() {
        this.lastUsed = System.currentTimeMillis();
    }

    public boolean isConnected() {
        return sftp != null && sftp.isConnected();
    }
}
