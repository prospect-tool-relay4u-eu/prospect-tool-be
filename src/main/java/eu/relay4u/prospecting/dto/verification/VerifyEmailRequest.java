package eu.relay4u.prospecting.dto.verification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Kod musi składać się z 6 cyfr") String code
) {}
