package eu.relay4u.prospecting.service.project_member;

import eu.relay4u.prospecting.dto.project_member.InviteMemberToProject;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.ProjectMemberStatus;
import eu.relay4u.prospecting.repository.ProjectMemberInvitationRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;

public class ProjectMemberInvitationServiceImpl implements ProjectMemberInvitationService {

    private ProjectMemberInvitationRepository projectMemberInvitationRepository;
    private ProjectRepository projectRepository;

    @Override
    public void inviteMemberToProject(Long projectID, InviteMemberToProject request) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new ProjectNotFoundException());

        ProjectMember member = new ProjectMember();

        member.setProject(project);
        member.setInvitedEmail(request.email());
        member.setRole(request.role());
        member.setStatus(ProjectMemberStatus.PENDING);

        projectMemberInvitationRepository.save(member);
    }

    @Override
    public void acceptInvitation(Long projectID) {
    }
}
