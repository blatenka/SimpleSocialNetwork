package com.aloute.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_infos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {

    @Id
    private Long id; 
    
    @OneToOne
    @MapsId 
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String fullname;

    @Column(length = 50)
    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dob;

    @Column(length = 100)
    private String hometown;
}