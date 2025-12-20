// src/main/java/com/ntu/cloudgui/cloudlb/core/Request.java
package com.ntu.cloudgui.cloudlb.core;

public class Request {

    public enum Type { UPLOAD, DOWNLOAD }

    private final String id;
    private final Type type;
    private final long sizeBytes;
    private final int basePriority;        // 0 = normal, higher = more important
    private final long arrivalTimeMillis;  // for aging

    // Mutable scheduling fields
    private long lastScheduledTimeMillis;
    private long remainingBytes;

    public Request(String id, Type type, long sizeBytes, int basePriority) {
        this.id = id;
        this.type = type;
        this.sizeBytes = sizeBytes;
        this.basePriority = basePriority;
        this.arrivalTimeMillis = System.currentTimeMillis();
        this.lastScheduledTimeMillis = this.arrivalTimeMillis;
        this.remainingBytes = sizeBytes;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getBasePriority() {
        return basePriority;
    }

    public long getArrivalTimeMillis() {
        return arrivalTimeMillis;
    }

    public long getLastScheduledTimeMillis() {
        return lastScheduledTimeMillis;
    }

    public void setLastScheduledTimeMillis(long lastScheduledTimeMillis) {
        this.lastScheduledTimeMillis = lastScheduledTimeMillis;
    }

    public long getRemainingBytes() {
        return remainingBytes;
    }

    public void setRemainingBytes(long remainingBytes) {
        this.remainingBytes = remainingBytes;
    }
}
