package eu.relay4u.prospecting.dto.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(
        @NotBlank String type,
        @NotBlank String title,
        @NotBlank String message,
        String resourceLink) {
}
