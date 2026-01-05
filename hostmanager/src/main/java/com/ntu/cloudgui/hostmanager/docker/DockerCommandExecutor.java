package com.ntu.cloudgui.hostmanager.docker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes Docker commands via ProcessBuilder
 * Handles all Docker CLI operations (run, stop, restart, etc.)
 */
public class DockerCommandExecutor {
    
    private static final Logger logger = LogManager.getLogger(DockerCommandExecutor.class);
    private static final int COMMAND_TIMEOUT = 30;  // seconds

    /**
     * Run a new Docker container
     */
    public ProcessResult runContainer(String containerName, int port, String image) {
        try {
            logger.info("Starting container: {} on port {}", containerName, port);

            List<String> command = new DockerCommandBuilder()
                .withCommand("run")
                .withDetachedMode()
                .withName(containerName)
                .withNetwork("soft40051_network")
                .withPortMapping(port, 22)
                .withImage(image)
                .build();
            
            ProcessResult result = executeWithTimeout(command, COMMAND_TIMEOUT);
            
            if (result.getExitCode() == 0) {
                logger.info("Container started successfully: {}", containerName);
            } else {
                logger.error("Failed to start container: {}", result.getError());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error running container", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }

    /**
     * List running containers matching a base name
     */
    public ProcessResult listContainersByName(String baseName) {
        try {
            List<String> command = Arrays.asList("docker", "ps", "-a", "--filter", "name=" + baseName, "--format", "{{.Names}}");
            return executeWithTimeout(command, 10);
        } catch (Exception e) {
            logger.error("Error listing containers by name", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Stop a running container
     */
    public ProcessResult stopContainer(String containerName) {
        try {
            logger.info("Stopping container: {}", containerName);
            
            List<String> command = new DockerCommandBuilder()
                .withCommand("stop")
                .withContainerName(containerName)
                .build();
            
            ProcessResult result = executeWithTimeout(command, COMMAND_TIMEOUT);
            
            if (result.getExitCode() == 0) {
                logger.info("Container stopped: {}", containerName);
                // Also remove the container
                removeContainer(containerName);
            } else {
                logger.error("Failed to stop container: {}", result.getError());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error stopping container", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Remove a container
     */
    public ProcessResult removeContainer(String containerName) {
        try {
            List<String> command = new DockerCommandBuilder()
                .withCommand("rm")
                .withContainerName(containerName)
                .build();
            
            ProcessResult result = executeWithTimeout(command, 10);
            
            if (result.getExitCode() == 0) {
                logger.info("Container removed: {}", containerName);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error removing container", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Restart a container
     */
    public ProcessResult restartContainer(String containerName) {
        try {
            logger.info("Restarting container: {}", containerName);
            
            List<String> command = new DockerCommandBuilder()
                .withCommand("restart")
                .withContainerName(containerName)
                .build();
            
            ProcessResult result = executeWithTimeout(command, COMMAND_TIMEOUT);
            
            if (result.getExitCode() == 0) {
                logger.info("Container restarted: {}", containerName);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error restarting container", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Inspect container details
     */
    public ProcessResult inspectContainer(String containerName) {
        try {
            List<String> command = new DockerCommandBuilder()
                .withCommand("inspect")
                .withContainerName(containerName)
                .build();
            
            return executeWithTimeout(command, 10);
            
        } catch (Exception e) {
            logger.error("Error inspecting container", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Get container logs
     */
    public ProcessResult getContainerLogs(String containerName) {
        try {
            List<String> command = Arrays.asList("docker", "logs", containerName);
            
            return executeWithTimeout(command, 10);
            
        } catch (Exception e) {
            logger.error("Error getting container logs", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Check if container exists
     */
    public boolean containerExists(String containerName) {
        try {
            List<String> command = Arrays.asList("docker", "ps", "-a", "--filter", 
                                                 "name=" + containerName, "-q");
            
            ProcessResult result = executeWithTimeout(command, 5);
            
            return result.getExitCode() == 0 && !result.getOutput().trim().isEmpty();
            
        } catch (Exception e) {
            logger.error("Error checking container existence", e);
            return false;
        }
    }
    
    /**
     * List all running containers
     */
    public ProcessResult listContainers() {
        try {
            List<String> command = Arrays.asList("docker", "ps", "--format", "{{.Names}}");
            
            return executeWithTimeout(command, 10);
            
        } catch (Exception e) {
            logger.error("Error listing containers", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Execute docker command with timeout
     */
    private ProcessResult executeWithTimeout(List<String> command, int timeoutSeconds) {
        try {
            logger.debug("Executing command: {}", String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for completion with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                logger.error("Command timed out after {} seconds", timeoutSeconds);
                return new ProcessResult(1, output.toString(), "Command timed out");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0) {
                logger.warn("Command failed with exit code {}: {}", exitCode, output);
            }
            
            return new ProcessResult(exitCode, output.toString(), "");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Command interrupted", e);
            return new ProcessResult(1, "", "Command interrupted: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error executing command", e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    /**
     * Get Docker version (for testing)
     */
    public String getDockerVersion() {
        try {
            ProcessResult result = executeWithTimeout(Arrays.asList("docker", "--version"), 5);
            return result.getExitCode() == 0 ? result.getOutput().trim() : "Unknown";
        } catch (Exception e) {
            logger.error("Error getting Docker version", e);
            return "Unknown";
        }
    }
}
