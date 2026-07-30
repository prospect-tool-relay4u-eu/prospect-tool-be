package eu.relay4u.prospecting.dto.project_member;

import eu.relay4u.prospecting.model.ProjectMemberRole;
import jakarta.validation.constraints.NotBlank;

public record InviteMemberToProjectDto (
        @NotBlank String email,
        ProjectMemberRole role
) {
}
