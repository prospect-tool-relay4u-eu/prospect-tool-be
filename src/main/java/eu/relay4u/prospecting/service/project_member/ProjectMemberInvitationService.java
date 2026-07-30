package eu.relay4u.prospecting.service.project_member;

import eu.relay4u.prospecting.dto.project_member.InviteMemberToProjectDto;
import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.User;

public interface ProjectMemberInvitationService {
    ProjectMember inviteMemberToProject(Long projectID, InviteMemberToProjectDto request, User user);
    void acceptInvitation(Long projectID);
}
