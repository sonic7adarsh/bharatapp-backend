package com.bharatshop.repository;

import com.bharatshop.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {

    List<NotificationLog> findByEventId(String eventId);

    List<NotificationLog> findByProvider(String provider);

    List<NotificationLog> findByChannel(String channel);

    List<NotificationLog> findByStatus(String status);

    List<NotificationLog> findByRecipient(String recipient);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.createdAt > :since")
    List<NotificationLog> findByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.sentAt BETWEEN :startTime AND :endTime")
    List<NotificationLog> findBySentAtBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.status = :status AND nl.createdAt > :since")
    List<NotificationLog> findByStatusAndCreatedAtAfter(
            @Param("status") String status,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.status = 'SENT' AND nl.createdAt > :since")
    long countSuccessfulByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.status = 'FAILED' AND nl.createdAt > :since")
    long countFailedByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT nl FROM NotificationLog nl WHERE (nl.recipient LIKE %:searchTerm% OR nl.message LIKE %:searchTerm% OR nl.errorMessage LIKE %:searchTerm%)")
    List<NotificationLog> searchLogs(@Param("searchTerm") String searchTerm);
}