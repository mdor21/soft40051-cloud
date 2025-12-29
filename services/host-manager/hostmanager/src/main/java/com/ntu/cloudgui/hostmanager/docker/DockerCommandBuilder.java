package com.ntu.cloudgui.hostmanager.docker;

import com.ntu.cloudgui.hostmanager.config.DockerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds concrete Docker CLI commands used by DockerService.
 */
public class DockerCommandBuilder {

    private static final Logger logger = LogManager.getLogger(DockerCommandBuilder.class);

    /**
     * Build docker run command
     */
    public List<String> buildRunCommand(String containerName, int port, String image) {
        List<String> command = new ArrayList<>();

        command.add(DockerConstants.DOCKER_EXECUTABLE);
        command.add("run");
        command.add("-it");                          // Interactive + TTY
        command.add("-d");                           // Detached mode
        command.add("--name");
        command.add(containerName);

        // Volume mount
        String volumePath = DockerConstants.CONTAINER_VOLUME_BASE + containerName + ":" +
                          DockerConstants.CONTAINER_DATA_PATH;
        command.add("-v");
        command.add(volumePath);

        // Port mapping
        String portMapping = port + ":" + DockerConstants.CONTAINER_PORT_INTERNAL;
        command.add("-p");
        command.add(portMapping);

        // Environment variables
        command.add("-e");
        command.add("CONTAINER_ID=" + containerName);
        command.add("-e");
        command.add("CONTAINER_PORT=" + port);

        // Resource limits (optional)
        command.add("--memory=512m");
        command.add("--cpus=0.5");

        // Image
        command.add(image != null ? image : DockerConstants.IMAGE_NAME);

        logger.debug("Built docker run command: {}", String.join(" ", command));
        return command;
    }

    /**
     * Build docker stop command
     */
    public List<String> buildStopCommand(String containerName) {
        return Arrays.asList(
            DockerConstants.DOCKER_EXECUTABLE,
            "stop",
            "-t",
            String.valueOf(DockerConstants.CONTAINER_SHUTDOWN_TIMEOUT),
            containerName
        );
    }

    /**
     * Build docker remove command
     */
    public List<String> buildRemoveCommand(String containerName) {
        return Arrays.asList(
            DockerConstants.DOCKER_EXECUTABLE,
            "rm",
            "-f",
            containerName
        );
    }

    /**
     * Build docker restart command
     */
    public List<String> buildRestartCommand(String containerName) {
        return Arrays.asList(
            DockerConstants.DOCKER_EXECUTABLE,
            "restart",
            "-t",
            String.valueOf(DockerConstants.CONTAINER_SHUTDOWN_TIMEOUT),
            containerName
        );
    }

    /**
     * Build docker inspect command
     */
    public List<String> buildInspectCommand(String containerName) {
        return Arrays.asList(
            DockerConstants.DOCKER_EXECUTABLE,
            "inspect",
            containerName
        );
    }

    /**
     * Build docker logs command
     */
    public List<String> buildLogsCommand(String containerName) {
        return Arrays.asList(
            DockerConstants.DOCKER_EXECUTABLE,
            "logs",
            "--tail", "100",
            containerName
        );
    }

    /**
     * Build docker stats command
     */
    public List<String> buildStatsCommand(String containerName) {
        return Arrays.asList(
            DockerConstants.DOCKER_EXECUTABLE,
            "stats",
            "--no-stream",
            "--format",
            "{{.CPUPerc}}|{{.MemUsage}}",
            containerName
        );
    }
}