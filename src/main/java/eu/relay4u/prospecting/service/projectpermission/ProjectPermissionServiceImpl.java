package eu.relay4u.prospecting.service.projectpermission;

import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.ProjectMemberRole;
import eu.relay4u.prospecting.model.ProjectMemberStatus;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectPermissionServiceImpl implements ProjectPermissionService {

    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public void checkReadPermission(Long projectId, User user) {
        ProjectMember member = getAcceptedMembership(projectId, user);

        if (!member.getRole().isReadAllowed()) {
            throw new AccessDeniedException("Read permission denied");
        }
    }

    @Override
    public void checkEditFieldsPermission(Long projectId, User user) {
        ProjectMember member = getAcceptedMembership(projectId, user);

        if (!member.getRole().isFieldEditingAllowed()) {
            throw new AccessDeniedException("Edit fields permission denied");
        }
    }

    @Override
    public void checkEditRecordsPermission(Long projectId, User user) {
        ProjectMember member = getAcceptedMembership(projectId, user);

        if (!member.getRole().isRecordEditingAllowed()) {
            throw new AccessDeniedException("Edit records permission denied");
        }
    }

    @Override
    public void checkOwnerPermission(Long projectId, User user) {
        ProjectMember member = getAcceptedMembership(projectId, user);

        if (member.getRole() != ProjectMemberRole.OWNER) {
            throw new AccessDeniedException("Owner permission required");
        }
    }

    private ProjectMember getAcceptedMembership(Long projectId, User user) {
        return projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                        projectId, user.getId(), ProjectMemberStatus.ACCEPTED)
                .orElseThrow(() -> new AccessDeniedException("You do not have access "
                        + "to this project"));
    }
}
