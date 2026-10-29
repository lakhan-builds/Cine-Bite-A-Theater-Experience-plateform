package com.cfs.BMS.entity;


import com.cfs.BMS.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    private LocalDateTime createdAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer rewardPoints = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Role role = Role.USER;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = Role.USER;
        }
        if (this.rewardPoints == null) {
            this.rewardPoints = 0;
        }
        this.createdAt=LocalDateTime.now();
    }

//    @PrePersist
//    protected void onCreate()
//    {
//        this.createdAt=LocalDateTime.now();
//    }




}
