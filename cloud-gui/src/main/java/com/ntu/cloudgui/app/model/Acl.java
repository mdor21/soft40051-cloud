package com.ntu.cloudgui.app.model;

public class Acl {
    private String fileId;
    private String username;
    private String permission;

    // Getters and setters
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
