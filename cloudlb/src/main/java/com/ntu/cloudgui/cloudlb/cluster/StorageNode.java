// com/ntu/cloudgui/cloudlb/cluster/StorageNode.java
package com.ntu.cloudgui.cloudlb.cluster;

public class StorageNode {

    public enum Status { HEALTHY, UNHEALTHY }

    private final String id;
    private final String host;
    private final int port;
    private volatile Status status;
    private volatile int currentLoad; // number of active requests, or similar

    public StorageNode(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.status = Status.HEALTHY;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getCurrentLoad() { return currentLoad; }
    public void incrementLoad() { currentLoad++; }
    public void decrementLoad() { currentLoad--; }
}
