package eu.relay4u.prospecting.dto.project_member;

import eu.relay4u.prospecting.model.ProjectMemberRole;

public record InviteMemberToProject(
        String email,
        ProjectMemberRole role
) {
}
