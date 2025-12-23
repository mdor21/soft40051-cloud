package com.ntu.cloudgui.aggservice.config;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.Getter;

import java.io.InputStream;
import java.io.OutputStream;

@Getter
public class SftpConnection {

    private final Session session;
    private final ChannelSftp channel;
    private final String host;
    private long lastUsed;

    public SftpConnection(Session session, ChannelSftp channel, String host) {
        this.session = session;
        this.channel = channel;
        this.host = host;
        this.lastUsed = System.currentTimeMillis();
    }

    public boolean isConnected() {
        return session != null && session.isConnected()
                && channel != null && channel.isConnected();
    }

    public void disconnect() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public void updateLastUsed() {
        this.lastUsed = System.currentTimeMillis();
    }

    public void deleteFile(String remotePath) throws Exception {
        channel.rm(remotePath);
    }

    public void createDirectory(String remoteDir) throws Exception {
        channel.mkdir(remoteDir);
    }

    public void uploadFile(InputStream in, String remotePath) throws Exception {
        channel.put(in, remotePath);
    }

    public void downloadFile(String remotePath, OutputStream out) throws Exception {
        channel.get(remotePath, out);
    }
}
