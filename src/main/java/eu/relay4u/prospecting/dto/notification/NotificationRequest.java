package eu.relay4u.prospecting.dto.notification;

import eu.relay4u.prospecting.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequest(
        @NotNull NotificationType type,
        @NotBlank String title,
        @NotBlank String message,
        String resourceLink) {
}
