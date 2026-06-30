package eu.relay4u.prospecting.dto.verification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @Email @NotBlank String email
) {}
