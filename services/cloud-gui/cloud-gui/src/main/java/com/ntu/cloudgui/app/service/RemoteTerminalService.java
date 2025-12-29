package com.ntu.cloudgui.app.service;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;

public class RemoteTerminalService {

    public String executeOnHost(String host, int port,
                                String user, String password,
                                String command) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(5000);

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        channel.setOutputStream(outputStream);
        channel.connect();

        while (!channel.isClosed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
        channel.disconnect();
        session.disconnect();

        String result = outputStream.toString();
        LoggingService.getInstance().log(user, "REMOTE_CMD",
                "Executed on " + host + ": " + command, true);
        return result;
    }
}
