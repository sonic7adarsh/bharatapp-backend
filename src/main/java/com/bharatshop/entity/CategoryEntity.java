package com.bharatshop.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String icon;

    private Integer priority = 0;

    @Column(name = "is_global")
    private Boolean isGlobal = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Boolean getIsGlobal() { return isGlobal; }
    public void setIsGlobal(Boolean isGlobal) { this.isGlobal = isGlobal; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
