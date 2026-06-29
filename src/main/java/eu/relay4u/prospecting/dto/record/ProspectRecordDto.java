package eu.relay4u.prospecting.dto.record;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ProspectRecordDto(
        UUID id,
        Long projectId,
        Map<String, Object> values,
        LocalDateTime createdAt
) {
}
