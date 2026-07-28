package eu.relay4u.prospecting.service.project_member;

import eu.relay4u.prospecting.dto.project_member.InviteMemberToProject;
import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.ProjectMemberRole;
import eu.relay4u.prospecting.model.ProjectMemberStatus;
import eu.relay4u.prospecting.repository.ProjectMemberInvitationRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberInvitationServiceImplTest {

    @Mock
    private ProjectMemberInvitationRepository invitationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @InjectMocks
    private ProjectMemberInvitationServiceImpl invitationService;

    @Test
    void inviteMemberToProject_Ok() {

        Long projectId = 1L;

        Project project = new Project();
        project.setId(projectId);

        InviteMemberToProject invitation =
                new InviteMemberToProject("test@test.com", ProjectMemberRole.MEMBER);

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        invitationService.inviteMemberToProject(projectId, invitation);

        ArgumentCaptor<ProjectMember> captor =
                ArgumentCaptor.forClass(ProjectMember.class);

        verify(invitationRepository).save(captor.capture());

        ProjectMember projectMember = captor.getValue();

        assertEquals(project, projectMember.getProject());
        assertEquals(ProjectMemberRole.MEMBER, projectMember.getRole());
        assertEquals(ProjectMemberStatus.PENDING, projectMember.getStatus());
        assertEquals("test@test.com", projectMember.getInvitedEmail());
    }
}