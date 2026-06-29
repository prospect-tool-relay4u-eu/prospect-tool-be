package eu.relay4u.prospecting.dto.field;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderFieldsRequest(
        @NotEmpty List<UUID> fieldIds
) {
}
