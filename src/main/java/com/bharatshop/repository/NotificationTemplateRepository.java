package com.bharatshop.repository;

import com.bharatshop.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByTenantIdAndEventTypeAndChannelAndLanguageAndIsActive(
            String tenantId, String eventType, String channel, String language, Boolean isActive);

    List<NotificationTemplate> findByTenantIdAndEventTypeAndIsActive(String tenantId, String eventType, Boolean isActive);

    List<NotificationTemplate> findByTenantIdAndChannelAndIsActive(String tenantId, String channel, Boolean isActive);

    List<NotificationTemplate> findByTenantIdAndIsActive(String tenantId, Boolean isActive);

    List<NotificationTemplate> findByEventTypeAndChannelAndLanguageAndIsActive(String eventType, String channel, String language, Boolean isActive);

    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.tenantId = :tenantId AND nt.eventType = :eventType AND nt.channel = :channel AND nt.isActive = true")
    List<NotificationTemplate> findActiveTemplates(
            @Param("tenantId") String tenantId,
            @Param("eventType") String eventType,
            @Param("channel") String channel);

    Optional<NotificationTemplate> findByTenantIdAndTemplateName(String tenantId, String templateName);

    List<NotificationTemplate> findByEventType(String eventType);

    List<NotificationTemplate> findByChannel(String channel);

    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.tenantId = :tenantId AND nt.isActive = true AND (nt.eventType LIKE %:searchTerm% OR nt.templateName LIKE %:searchTerm% OR nt.subject LIKE %:searchTerm%)")
    List<NotificationTemplate> searchTemplates(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
}