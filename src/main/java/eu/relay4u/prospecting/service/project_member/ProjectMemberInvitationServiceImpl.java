package eu.relay4u.prospecting.service.project_member;

import eu.relay4u.prospecting.dto.project_member.InviteMemberToProjectDto;
import eu.relay4u.prospecting.exception.*;
import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.ProjectMemberStatus;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.ProjectMemberRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;
import eu.relay4u.prospecting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberInvitationServiceImpl implements ProjectMemberInvitationService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
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

        return projectMemberRepository.save(member);
    }

    @Override
    @Transactional
    public ProjectMember acceptInvitation(Long projectID, String invitedEmail) {

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndInvitedEmail(projectID, invitedEmail)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found for project ID: "
                        + projectID + " and email: " + invitedEmail + "."));

        if (userRepository.existsByEmail(invitedEmail)) {
            member.setUser(userRepository.findByEmail(invitedEmail));
        } else {
            member.setUser(new User());
        }

        member.setStatus(ProjectMemberStatus.ACCEPTED);

        return member;
    }

    @Override
    @Transactional
    public ProjectMember declineInvitation(Long projectID, String invitedEmail) {

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndInvitedEmail(projectID, invitedEmail)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found for project ID: "
                        + projectID + " and email: " + invitedEmail + "."));

        member.setStatus(ProjectMemberStatus.DECLINED);

        return member;
    }
}
