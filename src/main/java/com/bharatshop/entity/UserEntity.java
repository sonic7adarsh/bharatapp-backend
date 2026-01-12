package com.bharatshop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String tenantId;
    
    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserRoleEntity> userRoles = new ArrayList<>();
    
    @Column(name = "active_role_id")
    private String activeRoleId;
    private String password;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public List<UserRoleEntity> getUserRoles() { return userRoles; }
    public void setUserRoles(List<UserRoleEntity> userRoles) { this.userRoles = userRoles; }
    
    public String getActiveRoleId() { return activeRoleId; }
    public void setActiveRoleId(String activeRoleId) { this.activeRoleId = activeRoleId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}