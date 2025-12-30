package com.ntu.cloudgui.hostmanager.docker;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder class for creating Docker commands.
 */
public class DockerCommandBuilder {

    private final List<String> command;

    public DockerCommandBuilder() {
        this.command = new ArrayList<>();
        this.command.add("docker");
    }

    public DockerCommandBuilder withCommand(String command) {
        this.command.add(command);
        return this;
    }

    public DockerCommandBuilder withDetachedMode() {
        this.command.add("-d");
        return this;
    }

    public DockerCommandBuilder withName(String name) {
        this.command.add("--name");
        this.command.add(name);
        return this;
    }

    public DockerCommandBuilder withNetwork(String network) {
        this.command.add("--network");
        this.command.add(network);
        return this;
    }

    public DockerCommandBuilder withPortMapping(int hostPort, int containerPort) {
        this.command.add("-p");
        this.command.add(hostPort + ":" + containerPort);
        return this;
    }

    public DockerCommandBuilder withImage(String image) {
        this.command.add(image);
        return this;
    }

    public DockerCommandBuilder withContainerName(String containerName) {
        this.command.add(containerName);
        return this;
    }

    public List<String> build() {
        return command;
    }
}
