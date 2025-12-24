package com.ntu.cloudgui.hostmanager.config;

/**
 * Configuration for Docker.
 */
public class DockerConfig {

    private final String networkName;
    private final String imageName;
    private final String containerBaseName;
    private final int minPort;
    private final int maxContainers;

    public DockerConfig(String networkName, String imageName, String containerBaseName, int minPort, int maxContainers) {
        this.networkName = networkName;
        this.imageName = imageName;
        this.containerBaseName = containerBaseName;
        this.minPort = minPort;
        this.maxContainers = maxContainers;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getImageName() {
        return imageName;
    }

    public String getContainerBaseName() {
        return containerBaseName;
    }

    public int getMinPort() {
        return minPort;
    }

    public int getMaxContainers() {
        return maxContainers;
    }
}
