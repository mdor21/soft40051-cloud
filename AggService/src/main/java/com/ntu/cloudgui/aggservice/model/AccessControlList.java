package com.ntu.cloudgui.aggservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "access_control_list")
@Data
@NoArgsConstructor
public class AccessControlList {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String aclId;
    private String fileId;
    private String userId;
    private String permission;
}
