package eu.relay4u.prospecting.dto.project;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255) String name,
        @Size(max = 1000) String description
) {
}
