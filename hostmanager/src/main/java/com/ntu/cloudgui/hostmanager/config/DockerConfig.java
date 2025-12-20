package com.ntu.cloudgui.hostmanager.config;

/**
 * Holds configuration used by DockerService to create and
 * manage file server containers.
 */
public class DockerConfig {

    private final String imageName;
    private final String containerNamePrefix;
    private final int basePort;
    private final int maxContainers;

    public DockerConfig(String imageName,
                        String containerNamePrefix,
                        int basePort,
                        int maxContainers) {
        this.imageName = imageName;
        this.containerNamePrefix = containerNamePrefix;
        this.basePort = basePort;
        this.maxContainers = maxContainers;
    }

    public static DockerConfig fromEnvOrDefaults() {
        String image   = System.getenv().getOrDefault("FS_IMAGE", "fileserver:latest");
        String prefix  = System.getenv().getOrDefault("FS_PREFIX", "file-server-");
        int basePort   = Integer.parseInt(System.getenv().getOrDefault("FS_BASE_PORT", "9000"));
        int max        = Integer.parseInt(System.getenv().getOrDefault("FS_MAX", "4"));
        return new DockerConfig(image, prefix, basePort, max);
    }

    public String getImageName()          { return imageName; }
    public String getContainerNamePrefix(){ return containerNamePrefix; }
    public int getBasePort()              { return basePort; }
    public int getMaxContainers()         { return maxContainers; }
}
