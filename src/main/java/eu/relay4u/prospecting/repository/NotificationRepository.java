package eu.relay4u.prospecting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import eu.relay4u.prospecting.model.Notification;
import eu.relay4u.prospecting.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserAndIsRead(User user, boolean isRead, Pageable pageable);
    Page<Notification> findByUser(User user, Pageable pageable);
    long countByUserAndIsReadFalse(User user);

    @Modifying
    void deleteByCreatedAtBefore(LocalDateTime thresholdDate);

    /**
     * Marks all unread notifications for a specific user as read.
     *
     * @param userId the ID of the target user
     * @return the number of updated notifications (useful for logging,
     *         API responses, or updating unread badges in the UI)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * Marks a specific unread notification as read for the given user.
     *
     * @param notificationId the ID of the notification to update
     * @param userId the ID of the owner user
     * @return the number of updated notifications (1 if updated successfully,
     *         0 if not found, already read, or belongs to another user)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.notificationId = :notificationId " +
            "AND n.user.id = :userId " +
            "AND n.isRead = false")
    int markAsReadByNotificationIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Long userId);
}
