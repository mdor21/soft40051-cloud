package com.ntu.cloudgui.hostmanager.docker;

import com.ntu.cloudgui.hostmanager.config.DockerConfig;
import com.ntu.cloudgui.hostmanager.util.LogUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes Docker CLI commands via ProcessBuilder to manage
 * file server containers (run, stop, remove).
 */
public class DockerService {

    private final DockerConfig config;

    public DockerService(DockerConfig config) {
        this.config = config;
    }

    /**
     * Ensure that the total number of running file server containers
     * matches the desired count.
     */
    public void ensureDesiredCount(int desired) {
        List<String> running = listRunning();
        int current = running.size();

        LogUtil.info("DockerService.ensureDesiredCount desired=" + desired + " current=" + current);

        if (desired > current) {
            scaleUp(desired - current);
        } else if (desired < current) {
            scaleDown(current - desired);
        } else {
            LogUtil.info("Container count already at desired level.");
        }
    }

    /**
     * Start the given number of new containers (up to maxContainers).
     */
    public void scaleUp(int amount) {
        if (amount <= 0) {
            return;
        }

        List<String> running = listRunning();
        int current = running.size();
        int max = config.getMaxContainers();

        int possible = Math.min(amount, max - current);
        if (possible <= 0) {
            LogUtil.info("scaleUp requested " + amount + " but already at max " + max);
            return;
        }

        LogUtil.info("Scaling UP by " + possible + " containers");

        for (int i = 0; i < possible; i++) {
            int index = nextIndex();
            List<String> cmd = DockerCommandBuilder.buildRunCommand(config, index);
            runCommand(cmd);
        }
    }

    /**
     * Stop and remove the given number of containers, starting from the end
     * of the running list.
     */
    public void scaleDown(int amount) {
        if (amount <= 0) {
            return;
        }

        List<String> running = listRunning();
        int count = Math.min(amount, running.size());
        if (count == 0) {
            LogUtil.info("No containers to scale down.");
            return;
        }

        LogUtil.info("Scaling DOWN by " + count + " containers");

        for (int i = 0; i < count; i++) {
            String name = running.get(running.size() - 1 - i); // last N
            runCommand(DockerCommandBuilder.buildStopCommand(name));
            runCommand(DockerCommandBuilder.buildRemoveCommand(name));
        }
    }

    /**
     * List the names of all running containers matching the configured prefix.
     */
    private List<String> listRunning() {
        List<String> names = new ArrayList<>();
        try {
            List<String> cmd = DockerCommandBuilder.buildListCommand(config);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process process = pb.start();

            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.isBlank()) {
                        names.add(line.trim());
                    }
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                LogUtil.error("docker ps command failed with exit code " + exit, null);
            }
        } catch (IOException | InterruptedException e) {
            LogUtil.error("Error listing running containers", e);
            Thread.currentThread().interrupt();
        }
        return names;
    }

    /**
     * Compute next index for container name/port mapping.
     * Simple strategy: number of currently running containers + 1.
     */
    private int nextIndex() {
        return listRunning().size() + 1;
    }

    /**
     * Run a Docker CLI command and stream its output to stdout/stderr.
     */
    private void runCommand(List<String> command) {
        try {
            LogUtil.info("Running Docker command: " + String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("docker> " + line);
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                LogUtil.error("Docker command failed with exit code " + exit, null);
            }
        } catch (IOException | InterruptedException e) {
            LogUtil.error("Error running Docker command", e);
            Thread.currentThread().interrupt();
        }
    }
}
