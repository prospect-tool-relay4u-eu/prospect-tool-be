package eu.relay4u.prospecting.dto.notification;

import eu.relay4u.prospecting.model.NotificationType;
import eu.relay4u.prospecting.model.User;

import java.time.LocalDateTime;

public record NotificationDto(
        Long notificationId,
        NotificationType type,
        String title,
        String message,
        Boolean isRead,
        String resourceLink,
        LocalDateTime createdAt) {
}
