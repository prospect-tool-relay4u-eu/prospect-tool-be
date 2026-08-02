package eu.relay4u.prospecting.service.notification;

import eu.relay4u.prospecting.dto.notification.NotificationDto;
import eu.relay4u.prospecting.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<NotificationDto> getUserNotifications(User user, Boolean isRead, Pageable pageable);
    long getUnreadCount(User user);
    void markAsRead(Long notificationId, User user);
    void markAllAsRead(User user);
    void createNotification(User user, String type, String title, String message, String link);
}
