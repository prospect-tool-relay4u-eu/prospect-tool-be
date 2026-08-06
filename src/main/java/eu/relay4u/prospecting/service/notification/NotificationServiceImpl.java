package eu.relay4u.prospecting.service.notification;

import eu.relay4u.prospecting.dto.notification.NotificationDto;
import eu.relay4u.prospecting.dto.notification.NotificationRequest;
import eu.relay4u.prospecting.exception.NotificationNotFoundException;
import eu.relay4u.prospecting.model.Notification;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(User user, Boolean isRead, Pageable pageable) {
        Page<Notification> notifications = (isRead == null)
                ? notificationRepository.findByUser(user, pageable)
                : notificationRepository.findByUserAndIsRead(user, isRead, pageable);

        return notifications.map(n -> new NotificationDto(
                        n.getNotificationId(),
                        n.getType(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getIsRead(),
                        n.getResourceLink(),
                        n.getCreatedAt()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        int updatedRows = notificationRepository.markAsReadByNotificationIdAndUserId(notificationId, user.getId());

        if (updatedRows == 0) {
            throw new NotificationNotFoundException();
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        int updatedRows = notificationRepository.markAllAsReadByUserId(user.getId());

        if (updatedRows == 0) {
            throw new NotificationNotFoundException();
        }
    }

    @Override
    @Transactional
    public void createNotification(User user, NotificationRequest notificationRequest) {
        Notification notification = new Notification();
        notification.setType(notificationRequest.type());
        notification.setTitle(notificationRequest.title());
        notification.setMessage(notificationRequest.message());
        notification.setResourceLink(notificationRequest.resourceLink());
        notification.setUser(user);

        notificationRepository.save(notification);
    }
}
