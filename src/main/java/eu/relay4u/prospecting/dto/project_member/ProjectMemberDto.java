package eu.relay4u.prospecting.dto.project_member;

import eu.relay4u.prospecting.model.ProjectMemberRole;
import eu.relay4u.prospecting.model.ProjectMemberStatus;

public record ProjectMemberDto(
        Long id,
        String email,
        ProjectMemberRole role,
        ProjectMemberStatus status
) {
}
