package eu.relay4u.prospecting.service.project_member;

import eu.relay4u.prospecting.dto.project_member.InviteMemberToProjectDto;
import eu.relay4u.prospecting.exception.MemberAlreadyExistException;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.exception.UserNotOwnerException;
import eu.relay4u.prospecting.model.*;
import eu.relay4u.prospecting.repository.ProjectMemberRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberInvitationServiceImplTest {

    @Mock
    private ProjectMemberRepository invitationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private InviteMemberToProjectDto request;

    @InjectMocks
    private ProjectMemberInvitationServiceImpl invitationService;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.aUser();
        project = TestDataFactory.aProject(user);
    }

    @Test
    void inviteMemberToProject_Ok() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        when(invitationRepository.existsByInvitedEmail("bob@bob.com"))
                .thenReturn(false);

        request = new InviteMemberToProjectDto("bob@bob.com", ProjectMemberRole.MEMBER);

        invitationService.inviteMemberToProject(project.getId(), request , user);

        ArgumentCaptor<ProjectMember> captor =
                ArgumentCaptor.forClass(ProjectMember.class);

        verify(invitationRepository).save(captor.capture());

        ProjectMember projectMember = captor.getValue();

        assertEquals(project, projectMember.getProject());
        assertEquals(ProjectMemberRole.MEMBER, projectMember.getRole());
        assertEquals(ProjectMemberStatus.PENDING, projectMember.getStatus());
        assertEquals("bob@bob.com", projectMember.getInvitedEmail());
    }

    @Test
    void inviteMemberToProject_ProjectNotFound_notOk() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class,
                () -> invitationService.inviteMemberToProject(1L, request, user));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteMemberToProject_MemberAlreadyExists_notOk() {
        request = new InviteMemberToProjectDto("bob@bob.com", ProjectMemberRole.MEMBER);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        when(invitationRepository.existsByInvitedEmail(request.email()))
                .thenReturn(true);

        assertThrows(MemberAlreadyExistException.class,
                () -> invitationService.inviteMemberToProject(1L, request, user));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteMemberToProject_UserIsNotOwner_notOk() {
        User noOwnerUser = TestDataFactory.aUser();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(UserNotOwnerException.class,
                () -> invitationService.inviteMemberToProject(project.getId(), request, noOwnerUser));

        verify(invitationRepository, never()).save(any());
    }
}