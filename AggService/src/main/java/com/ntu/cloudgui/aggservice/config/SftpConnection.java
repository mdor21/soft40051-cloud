package com.ntu.cloudgui.aggservice.config;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
