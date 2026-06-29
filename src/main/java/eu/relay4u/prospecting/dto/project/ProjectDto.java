package eu.relay4u.prospecting.dto.project;

import eu.relay4u.prospecting.dto.field.FieldDefinitionDto;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectDto(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        List<FieldDefinitionDto> fields
) {
}
