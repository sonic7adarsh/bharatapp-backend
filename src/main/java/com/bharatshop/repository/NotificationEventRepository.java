package com.bharatshop.repository;

import com.bharatshop.entity.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, String> {

    List<NotificationEvent> findByTenantIdAndUserId(String tenantId, String userId);

    List<NotificationEvent> findByTenantId(String tenantId);

    List<NotificationEvent> findByUserId(String userId);

    List<NotificationEvent> findByStatus(String status);

    List<NotificationEvent> findByEventType(String eventType);

    @Query("SELECT ne FROM NotificationEvent ne WHERE ne.status = :status AND ne.scheduledAt < :scheduledTime")
    List<NotificationEvent> findByStatusAndScheduledAtBefore(
            @Param("status") String status, 
            @Param("scheduledTime") LocalDateTime scheduledTime);

    @Query("SELECT ne FROM NotificationEvent ne WHERE ne.tenantId = :tenantId AND ne.userId = :userId AND ne.eventType = :eventType AND ne.createdAt > :since")
    List<NotificationEvent> findRecentEventsByUserAndType(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId, 
            @Param("eventType") String eventType,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(ne) FROM NotificationEvent ne WHERE ne.tenantId = :tenantId AND ne.createdAt > :since")
    long countByTenantIdAndCreatedAtAfter(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    @Query("SELECT ne FROM NotificationEvent ne WHERE ne.tenantId = :tenantId AND ne.status = :status AND ne.priority = :priority")
    List<NotificationEvent> findByTenantIdAndStatusAndPriority(
            @Param("tenantId") String tenantId,
            @Param("status") String status,
            @Param("priority") String priority);

    Optional<NotificationEvent> findByIdAndTenantId(String id, String tenantId);
}