/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ntu.cloudgui.hostmanager.docker;

/**
 * Docker configuration constants
 */
public class DockerConstants {
    
    // Docker executable
    public static final String DOCKER_EXECUTABLE = getDockerPath();
    
    // Image and container configuration
    public static final String IMAGE_NAME = "ubuntu:latest";
    public static final String BASE_CONTAINER_NAME = "fileserver-";
    public static final int BASE_PORT = 8000;
    public static final int MAX_CONTAINERS = 10;
    public static final int MIN_CONTAINERS = 2;
    
    // Container parameters
    public static final int CONTAINER_STARTUP_TIMEOUT = 30;     // seconds
    public static final int CONTAINER_SHUTDOWN_TIMEOUT = 30;    // seconds
    public static final String CONTAINER_VOLUME_BASE = "/data/";
    public static final String CONTAINER_DATA_PATH = "/data";
    public static final String CONTAINER_PORT_INTERNAL = "8080";
    
    // Resource limits
    public static final String MEMORY_LIMIT = "512m";
    public static final String CPU_LIMIT = "0.5";
    
    // Health check configuration
    public static final int HEALTH_CHECK_TIMEOUT = 5;           // seconds
    public static final int HEALTH_CHECK_RETRY_COUNT = 3;
    
    /**
     * Get docker executable path based on OS
     */
    private static String getDockerPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "docker.exe";
        }
        return "docker";
    }
    
    private DockerConstants() {
        // Prevent instantiation
    }
}