package eu.relay4u.prospecting.service.project_member;

import eu.relay4u.prospecting.dto.project_member.InviteMemberToProjectDto;
import eu.relay4u.prospecting.exception.MemberAlreadyExistsException;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.exception.UserNotOwnerException;
import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.ProjectMemberStatus;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.ProjectMemberRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectMemberInvitationServiceImpl implements ProjectMemberInvitationService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;

    @Override
    public ProjectMember inviteMemberToProject(Long projectID,
                                               InviteMemberToProjectDto request, User user) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(ProjectNotFoundException::new);

        User owner = project.getOwner();

        if (owner == null || !owner.getId().equals(user.getId())) {
            throw new UserNotOwnerException("User with ID: " + user.getId()
                    + " is not the owner of project: " + project.getId());
        }

        if (projectMemberRepository
                .existsByProjectIdAndInvitedEmail(project.getId(), request.email())) {
            throw new MemberAlreadyExistsException("Member with email "
                    + request.email() + " already assigned to project: " + project.getId());
        }

        ProjectMember member = new ProjectMember();

        member.setProject(project);
        member.setInvitedEmail(request.email());
        member.setRole(request.role());
        member.setStatus(ProjectMemberStatus.PENDING);

        projectMemberRepository.save(member);

        return member;
    }

    @Override
    public void acceptInvitation(Long projectID) {
    }
}
