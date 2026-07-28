package eu.relay4u.prospecting.service.project_member;

import eu.relay4u.prospecting.dto.project_member.InviteMemberToProject;

public interface ProjectMemberInvitationService {
    void inviteMemberToProject(Long projectID, InviteMemberToProject inviteMemberToProject);
    void acceptInvitation(Long projectID);
}
