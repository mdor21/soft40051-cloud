package com.ntu.cloudgui.aggservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "access_control_list")
public class AccessControlList {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String aclId;
    private String fileId;
    private String userId;
    private String permission;

    public AccessControlList() {
    }

    public AccessControlList(String aclId, String fileId, String userId, String permission) {
        this.aclId = aclId;
        this.fileId = fileId;
        this.userId = userId;
        this.permission = permission;
    }

    // Getters and Setters
    public String getAclId() { return aclId; }
    public void setAclId(String aclId) { this.aclId = aclId; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
