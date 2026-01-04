package com.ntu.cloudgui.aggservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "access_control_list")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessControlList {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String aclId;
    private String fileId;
    private String userId;
    private String permission;
}
