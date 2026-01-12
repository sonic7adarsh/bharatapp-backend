package com.bharatshop.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.bharatshop.config.JsonToMapConverter;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {
    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "channel", length = 50, nullable = false)
    private String channel;

    @Column(name = "template_name", length = 255, nullable = false)
    private String templateName;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "variables", columnDefinition = "JSON")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> variables;

    @Column(name = "language", length = 10)
    private String language = "en";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    // Constructors
    public NotificationTemplate() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProvider() {
        // Map channel to provider
        return switch (channel) {
            case "WHATSAPP" -> "WHATSAPP";
            case "SMS" -> "MSG91"; // Default SMS provider
            case "EMAIL" -> "EMAIL";
            default -> "UNKNOWN";
        };
    }

    @Override
    public String toString() {
        return "NotificationTemplate{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", channel='" + channel + '\'' +
                ", templateName='" + templateName + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}