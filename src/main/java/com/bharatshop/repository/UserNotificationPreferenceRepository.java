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

    List<UserNotificationPreference> findByUserId(String userId);

    Optional<UserNotificationPreference> findByUserIdAndChannel(String userId, String channel);

    List<UserNotificationPreference> findByChannelAndEnabled(String channel, Boolean enabled);

    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.userId = :userId AND unp.channel = :channel AND unp.enabled = true")
    Optional<UserNotificationPreference> findActivePreference(
            @Param("userId") String userId,
            @Param("channel") String channel);

    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.userId = :userId AND unp.enabled = true")
    List<UserNotificationPreference> findAllEnabledForUser(@Param("userId") String userId);

    long countByChannelAndEnabled(String channel, Boolean enabled);
}