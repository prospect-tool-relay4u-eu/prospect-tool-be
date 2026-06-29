package eu.relay4u.prospecting.dto.field;

import eu.relay4u.prospecting.model.FieldType;

import java.util.UUID;

public record FieldDefinitionDto(
        UUID id,
        String key,
        String label,
        FieldType type,
        boolean required,
        int order
) {
}
