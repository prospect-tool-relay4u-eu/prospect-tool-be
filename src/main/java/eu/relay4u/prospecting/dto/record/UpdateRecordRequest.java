package eu.relay4u.prospecting.dto.record;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateRecordRequest(
        @NotNull Map<String, Object> values
) {
}
