package com.ticketing.ticket_reservation.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name="users")
@Getter 
@Setter 
@NoArgsConstructor 
@Builder 
@AllArgsConstructor 
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column (name="id")
    private Long id;

    @NotBlank @Column(nullable=false, name="full_name")
    private String fullName;

    @Email @NotBlank @Column(unique=true,nullable=false,name="email_id")
    private String email;

    private String password;

    @Enumerated(EnumType.STRING) @Column (name="auth_provider",nullable = false)
    private AuthProvider provider;

    @Enumerated (EnumType.STRING) @Column (nullable=false,name="role") 
    private Role role;

    @Column (name="created_at",updatable=false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    }


}
