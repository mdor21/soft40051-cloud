package com.ntu.cloudgui.aggservice.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class SftpConnectionPool {
    private final SftpConfig sftpConfig;
    private final ConcurrentLinkedQueue<Session> pool = new ConcurrentLinkedQueue<>();
    private static final int MAX_POOL_SIZE = 10;

    public SftpConnectionPool(SftpConfig sftpConfig) {
        this.sftpConfig = sftpConfig;
    }

    public Session getConnection() throws JSchException {
        Session session = pool.poll();
        if (session != null && session.isConnected()) {
            return session;
        }
        return createNewSession();
    }

    private Session createNewSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(
            sftpConfig.getUsername(),
            sftpConfig.getHost(),
            sftpConfig.getPort()
        );
        session.setPassword(sftpConfig.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        session.setServerAliveInterval(60000);
        session.connect(sftpConfig.getConnectionTimeout());
        return session;
    }

    public void returnConnection(Session session) {
        if (pool.size() < MAX_POOL_SIZE && session.isConnected()) {
            pool.offer(session);
        } else {
            session.disconnect();
        }
    }

    public void shutdown() {
        pool.forEach(Session::disconnect);
        pool.clear();
    }
}
