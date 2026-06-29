package eu.relay4u.prospecting.dto.field;

import eu.relay4u.prospecting.model.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFieldRequest(
        @NotBlank @Size(max = 100) String key,
        @NotBlank @Size(max = 255) String label,
        @NotNull FieldType type,
        boolean required,
        int order
) {
}
