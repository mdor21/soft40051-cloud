package com.ntu.cloudgui.hostmanager.docker;

import com.ntu.cloudgui.hostmanager.config.DockerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds concrete Docker CLI commands used by DockerService.
 */
public class DockerCommandBuilder {

    /**
     * Build a "docker run" command for a new file server container.
     *
     * Example:
     *   docker run -d --name file-server-1 -p 9001:8080 fileserver:latest
     */
    public static List<String> buildRunCommand(DockerConfig config, int index) {
        String name = config.getContainerNamePrefix() + index;
        int hostPort = config.getBasePort() + index;   // e.g. 9000 + index

        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("run");
        cmd.add("-d");
        cmd.add("--name");
        cmd.add(name);
        cmd.add("-p");
        cmd.add(hostPort + ":8080");
        cmd.add(config.getImageName());
        return cmd;
    }

    /**
     * Build a "docker stop" command for the given container name.
     */
    public static List<String> buildStopCommand(String name) {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("stop");
        cmd.add(name);
        return cmd;
    }

    /**
     * Build a "docker rm" command for the given container name.
     */
    public static List<String> buildRemoveCommand(String name) {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("rm");
        cmd.add(name);
        return cmd;
    }

    /**
     * Build a "docker ps" command that lists container names
     * matching the configured name prefix.
     *
     * Example:
     *   docker ps --format "{{.Names}}" --filter "name=file-server-"
     */
    public static List<String> buildListCommand(DockerConfig config) {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("ps");
        cmd.add("--format");
        cmd.add("{{.Names}}");
        cmd.add("--filter");
        cmd.add("name=" + config.getContainerNamePrefix());
        return cmd;
    }

    private DockerCommandBuilder() {
        // utility class; no instances
    }
}
