package com.ntu.cloudgui.hostmanager.scaling;

import com.ntu.cloudgui.hostmanager.container.ContainerInfo;
import com.ntu.cloudgui.hostmanager.container.ContainerManager;
import com.ntu.cloudgui.hostmanager.docker.DockerCommandExecutor;
import com.ntu.cloudgui.hostmanager.docker.ProcessResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handles the logic for scaling containers up and down.
 */
public class ScalingLogic {

    private static final Logger logger = LogManager.getLogger(ScalingLogic.class);
    private static final String CONTAINER_BASE_NAME = "soft40051-files-container";
    private static final String IMAGE_NAME = "lscr.io/linuxserver/openssh-server:latest";
    private static final String NETWORK_NAME = "soft40051_network";
    private static final int MIN_PORT = 4848;
    private static final int MAX_CONTAINERS = 4;
    private static final String STORAGE_BASE_DIR_ENV = "STORAGE_BASE_DIR";

    private final ContainerManager containerManager;
    private final DockerCommandExecutor dockerExecutor;
    private ScalingEventPublisher eventPublisher;

    public ScalingLogic(ContainerManager containerManager, DockerCommandExecutor dockerExecutor) {
        this.containerManager = containerManager;
        this.dockerExecutor = dockerExecutor;
    }

    public void setEventPublisher(ScalingEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handles a scale-up request.
     * Finds the next available container numbers and creates them.
     *
     * @param count The number of containers to add.
     */
    public void handleScaleUp(int count) {
        logger.info("Handling scale-up request for {} containers", count);
        Set<String> activeContainers = containerManager.getAllContainers().stream()
                .map(ContainerInfo::getContainerName)
                .collect(Collectors.toSet());
        long currentSize = activeContainers.size();

        if (currentSize >= MAX_CONTAINERS) {
            logger.warn("Cannot scale up, already at maximum capacity of {}", MAX_CONTAINERS);
            return;
        }

        long spaceAvailable = MAX_CONTAINERS - currentSize;
        long containersToStart = Math.min(count, spaceAvailable);

        IntStream.rangeClosed(1, MAX_CONTAINERS)
                .mapToObj(i -> CONTAINER_BASE_NAME + i)
                .filter(name -> !activeContainers.contains(name))
                .limit(containersToStart)
                .forEach(this::startContainer);
    }

    /**
     * Handles a scale-down request.
     * Removes the highest-numbered containers first.
     *
     * @param count The number of containers to remove.
     */
    public void handleScaleDown(int count) {
        logger.info("Handling scale-down request for {} containers", count);
        if (count <= 0) return;

        List<String> containersToStop = containerManager.getAllContainers().stream()
                .map(ContainerInfo::getContainerName)
                .sorted(Comparator.comparingInt(this::getContainerNumber).reversed())
                .limit(count)
                .collect(Collectors.toList());

        if (containersToStop.isEmpty()) {
            logger.info("No containers to scale down.");
            return;
        }

        logger.info("Identified containers to stop: {}", containersToStop);
        containersToStop.forEach(this::stopContainer);
    }

    public void handleScaleForNode(String action, int nodeIndex) {
        if (nodeIndex < 1 || nodeIndex > MAX_CONTAINERS) {
            logger.warn("Invalid node index {} (valid range: 1-{})", nodeIndex, MAX_CONTAINERS);
            return;
        }

        String containerName = CONTAINER_BASE_NAME + nodeIndex;
        if ("up".equalsIgnoreCase(action)) {
            startContainer(containerName);
        } else if ("down".equalsIgnoreCase(action)) {
            stopContainer(containerName);
        } else {
            logger.warn("Unknown scaling action for node: {}", action);
        }
    }

    private void startContainer(String containerName) {
        try {
            if (dockerExecutor.containerExists(containerName)) {
                if (dockerExecutor.isContainerRunning(containerName)) {
                    logger.info("Container already running, skipping start: {}", containerName);
                    return;
                }
                ProcessResult startResult = dockerExecutor.startContainer(containerName);
                if (startResult.getExitCode() == 0) {
                    containerManager.addContainer(containerName);
                    if (eventPublisher != null) {
                        eventPublisher.publishScalingEvent("up", containerName);
                    }
                }
                return;
            }

            int containerNumber = getContainerNumber(containerName);
            int port = MIN_PORT + containerNumber - 1;
            Map<String, String> envVars = buildStorageEnvVars();
            Map<String, String> volumes = buildStorageVolumes(containerNumber);
            ProcessResult result = dockerExecutor.runContainer(containerName, port, IMAGE_NAME, NETWORK_NAME, envVars, volumes);
            if (result.getExitCode() == 0) {
                containerManager.addContainer(containerName);
                if (eventPublisher != null) {
                    eventPublisher.publishScalingEvent("up", containerName);
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Could not parse container number from name: {}", containerName, e);
        }
    }

    private void stopContainer(String containerName) {
        ProcessResult result = dockerExecutor.stopContainer(containerName);
        if (result.getExitCode() == 0) {
            containerManager.removeContainer(containerName);
            if (eventPublisher != null) {
                eventPublisher.publishScalingEvent("down", containerName);
            }
        }
    }

    private int getContainerNumber(String containerName) {
        return Integer.parseInt(containerName.substring(CONTAINER_BASE_NAME.length()));
    }

    private Map<String, String> buildStorageEnvVars() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("PUID", getEnvOrDefault("PUID", "1000"));
        envVars.put("PGID", getEnvOrDefault("PGID", "1000"));
        envVars.put("TZ", getEnvOrDefault("TZ", "Etc/UTC"));
        envVars.put("USER_NAME", getEnvOrDefault("SFTP_USER", "ntu-user"));

        String sftpPass = System.getenv("SFTP_PASS");
        if (sftpPass == null || sftpPass.isBlank()) {
            logger.warn("SFTP_PASS is not set; storage container will start with an empty password");
            sftpPass = "";
        }
        envVars.put("USER_PASSWORD", sftpPass);
        envVars.put("SUDO_ACCESS", "true");

        return envVars;
    }

    private Map<String, String> buildStorageVolumes(int containerNumber) {
        String baseDir = resolveStorageBaseDir();
        Path storagePath = Paths.get(baseDir, "node" + containerNumber).toAbsolutePath().normalize();
        Map<String, String> volumes = new HashMap<>();
        volumes.put(storagePath.toString(), "/data");
        return volumes;
    }

    private String resolveStorageBaseDir() {
        String envDir = System.getenv(STORAGE_BASE_DIR_ENV);
        if (envDir != null && !envDir.isBlank()) {
            return envDir;
        }

        if (Files.isDirectory(Paths.get("storage"))) {
            return "storage";
        }
        if (Files.isDirectory(Paths.get("..", "storage"))) {
            return Paths.get("..", "storage").toString();
        }
        return "storage";
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
