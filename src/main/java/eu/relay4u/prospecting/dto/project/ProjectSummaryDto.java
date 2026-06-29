package eu.relay4u.prospecting.dto.project;

import java.time.LocalDateTime;

public record ProjectSummaryDto(
        Long id,
        String name,
        String description,
        long fieldCount,
        long recordCount,
        LocalDateTime createdAt
) {
}
