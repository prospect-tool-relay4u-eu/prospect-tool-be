package eu.relay4u.prospecting.dto.project_member;

import eu.relay4u.prospecting.model.ProjectMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberToProjectDto (
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required")
        ProjectMemberRole role
) {
}
