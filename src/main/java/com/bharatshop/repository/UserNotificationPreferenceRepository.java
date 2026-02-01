package com.bharatshop.repository;

import com.bharatshop.entity.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, String> {

    List<UserNotificationPreference> findByTenantIdAndUserId(String tenantId, String userId);

    Optional<UserNotificationPreference> findByTenantIdAndUserIdAndChannel(String tenantId, String userId, String channel);

    List<UserNotificationPreference> findByTenantIdAndChannelAndEnabled(String tenantId, String channel, Boolean enabled);

    List<UserNotificationPreference> findByUserIdAndChannel(String userId, String channel);

    List<UserNotificationPreference> findByTenantIdAndEnabled(String tenantId, Boolean enabled);

    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.tenantId = :tenantId AND unp.userId = :userId AND unp.channel = :channel AND unp.enabled = true")
    Optional<UserNotificationPreference> findActivePreference(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("channel") String channel);

    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.tenantId = :tenantId AND unp.enabled = true")
    List<UserNotificationPreference> findAllEnabledForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.userId = :userId AND unp.enabled = true")
    List<UserNotificationPreference> findAllEnabledForUser(@Param("userId") String userId);

    long countByTenantIdAndEnabled(String tenantId, Boolean enabled);

    long countByChannelAndEnabled(String channel, Boolean enabled);
}