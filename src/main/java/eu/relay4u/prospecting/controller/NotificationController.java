package eu.relay4u.prospecting.controller;

import eu.relay4u.prospecting.dto.notification.NotificationDto;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getUserNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean isRead,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationDto> notifications = notificationService.getUserNotifications(currentUser, isRead, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User currentUser) {
        long unreadCount = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(unreadCount);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("id") Long notificationId,
            @AuthenticationPrincipal User currentUser) {

        notificationService.markAsRead(notificationId, currentUser);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.noContent().build();
    }
}
