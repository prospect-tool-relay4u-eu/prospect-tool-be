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
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    void deleteByCreatedAtBefore(LocalDateTime thresholdDate);
}
