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
    @Column(name = "alternate_phone")
    private String alternatePhone;
    // Legacy single role (kept for backward compatibility where referenced)
    private String role;

    // MVP v1: store multiple roles directly on User via CSV to avoid extra tables
    @Column(name = "roles", length = 1024)
    private String rolesCsv; // e.g., "CUSTOMER,SELLER"

    @Column(name = "status")
    private String status; // ACTIVE / DISABLED

    // Derived flag for MVP: avoid schema change
    @jakarta.persistence.Transient
    private Boolean isActive;

    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserRoleEntity> userRoles = new ArrayList<>();

    @Column(name = "active_role_id")
    private String activeRoleId;

    @Column(name = "password_hash")
    private String passwordHash;

    private Instant createdAt;
    private Instant updatedAt;

    // Lifecycle hooks
    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) status = "ACTIVE";
        if (isActive == null) isActive = true;
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
    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public List<UserRoleEntity> getUserRoles() { return userRoles; }
    public void setUserRoles(List<UserRoleEntity> userRoles) { this.userRoles = userRoles; }
    
    public String getActiveRoleId() { return activeRoleId; }
    public void setActiveRoleId(String activeRoleId) { this.activeRoleId = activeRoleId; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    // Backward-compatible accessors used across existing services
    public String getPassword() { return passwordHash; }
    public void setPassword(String password) { this.passwordHash = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRolesCsv() { return rolesCsv; }
    public void setRolesCsv(String rolesCsv) { this.rolesCsv = rolesCsv; }

    public Boolean getIsActive() {
        // If explicit flag set, prefer it; otherwise derive from status
        if (isActive != null) return isActive;
        return status == null || status.equalsIgnoreCase("ACTIVE");
    }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // Keep backward-compatible password alias
    // Already defined above: setPassword(String) maps to passwordHash
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}