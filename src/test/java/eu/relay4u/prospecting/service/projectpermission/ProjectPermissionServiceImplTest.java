package eu.relay4u.prospecting.service.projectpermission;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.ProjectMemberRole;
import eu.relay4u.prospecting.model.ProjectMemberStatus;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.ProjectMemberRepository;
import eu.relay4u.prospecting.util.TestDataFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionServiceImplTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long USER_ID = 10L;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectPermissionServiceImpl projectPermissionService;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.aUser();
        project = TestDataFactory.aProject(user);
    }

    @Test
    void checkEditFieldsPermission_allowsOwner() {
        mockAcceptedMember(ProjectMemberRole.OWNER);

        assertThatCode(() -> projectPermissionService.checkEditFieldsPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkEditFieldsPermission_allowsAdmin() {
        mockAcceptedMember(ProjectMemberRole.ADMIN);

        assertThatCode(() -> projectPermissionService.checkEditFieldsPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkEditFieldsPermission_deniesMember() {
        mockAcceptedMember(ProjectMemberRole.MEMBER);

        assertThatThrownBy(() -> projectPermissionService.checkEditFieldsPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkEditFieldsPermission_deniesViewer() {
        mockAcceptedMember(ProjectMemberRole.VIEWER);

        assertThatThrownBy(() -> projectPermissionService.checkEditFieldsPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkEditRecordsPermission_allowsOwner() {
        mockAcceptedMember(ProjectMemberRole.OWNER);

        assertThatCode(() -> projectPermissionService.checkEditRecordsPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkEditRecordsPermission_allowsAdmin() {
        mockAcceptedMember(ProjectMemberRole.ADMIN);

        assertThatCode(() -> projectPermissionService.checkEditRecordsPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkEditRecordsPermission_allowsMember() {
        mockAcceptedMember(ProjectMemberRole.MEMBER);

        assertThatCode(() -> projectPermissionService.checkEditRecordsPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkEditRecordsPermission_deniesViewer() {
        mockAcceptedMember(ProjectMemberRole.VIEWER);

        assertThatThrownBy(() -> projectPermissionService.checkEditRecordsPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkReadPermission_allowsOwner() {
        mockAcceptedMember(ProjectMemberRole.OWNER);

        assertThatCode(() -> projectPermissionService.checkReadPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReadPermission_allowsViewer() {
        mockAcceptedMember(ProjectMemberRole.VIEWER);

        assertThatCode(() -> projectPermissionService.checkReadPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReadPermission_deniesUserWithoutMembership() {
        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectPermissionService.checkReadPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkReadPermission_deniesPendingMember() {
        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectPermissionService.checkReadPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectMemberRepository).findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED);
    }

    @Test
    void checkOwnerPermission_allowsOwner() {
        mockAcceptedMember(ProjectMemberRole.OWNER);

        assertThatCode(() -> projectPermissionService.checkOwnerPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkOwnerPermission_deniesAdmin() {
        mockAcceptedMember(ProjectMemberRole.ADMIN);

        assertThatThrownBy(() -> projectPermissionService.checkOwnerPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Owner permission required");
    }

    @Test
    void checkOwnerPermission_deniesMember() {
        mockAcceptedMember(ProjectMemberRole.MEMBER);

        assertThatThrownBy(() -> projectPermissionService.checkOwnerPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Owner permission required");
    }

    @Test
    void checkOwnerPermission_deniesViewer() {
        mockAcceptedMember(ProjectMemberRole.VIEWER);

        assertThatThrownBy(() -> projectPermissionService.checkOwnerPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Owner permission required");
    }

    @Test
    void checkReadPermission_deniesUserWithoutAcceptedMembership() {
        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectPermissionService.checkReadPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have access to this project");

        verify(projectMemberRepository).findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED);
    }

    @Test
    void checkEditFieldsPermission_deniesPendingMember() {
        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectPermissionService.checkEditFieldsPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have access to this project");
    }

    private void mockAcceptedMember(ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(ProjectMemberStatus.ACCEPTED);

        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED))
                .thenReturn(Optional.of(member));
    }

    @ParameterizedTest
    @EnumSource(ProjectMemberRole.class)
    void checkReadPermission_shouldAllowEveryAcceptedRole(ProjectMemberRole role) {
        mockAcceptedMembership(role);

        assertThatCode(() -> projectPermissionService.checkReadPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();

        verifyAcceptedMembershipQuery();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectMemberRole.class, names = {"OWNER", "ADMIN"})
    void checkEditFieldsPermission_shouldAllowOwnerAndAdmin(ProjectMemberRole role) {
        mockAcceptedMembership(role);

        assertThatCode(() -> projectPermissionService.checkEditFieldsPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectMemberRole.class, names = {"MEMBER", "VIEWER"})
    void checkEditFieldsPermission_shouldDenyMemberAndViewer(ProjectMemberRole role) {
        mockAcceptedMembership(role);

        assertThatThrownBy(() -> projectPermissionService
                .checkEditFieldsPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Edit fields permission denied");
    }

    @ParameterizedTest
    @EnumSource(value = ProjectMemberRole.class, names = {"OWNER", "ADMIN", "MEMBER"})
    void checkEditRecordsPermission_shouldAllowOwnerAdminAndMember(ProjectMemberRole role) {
        mockAcceptedMembership(role);

        assertThatCode(() -> projectPermissionService.checkEditRecordsPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @Test
    void checkEditRecordsPermission_shouldDenyViewer() {
        mockAcceptedMembership(ProjectMemberRole.VIEWER);

        assertThatThrownBy(() -> projectPermissionService
                .checkEditRecordsPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Edit records permission denied");
    }

    @Test
    void checkOwnerPermission_shouldAllowOwner() {
        mockAcceptedMembership(ProjectMemberRole.OWNER);

        assertThatCode(() -> projectPermissionService.checkOwnerPermission(PROJECT_ID, user))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectMemberRole.class, names = {"ADMIN", "MEMBER", "VIEWER"})
    void checkOwnerPermission_shouldDenyNonOwnerRoles(ProjectMemberRole role) {
        mockAcceptedMembership(role);

        assertThatThrownBy(() -> projectPermissionService.checkOwnerPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Owner permission required");
    }

    @Test
    void checkReadPermission_shouldDenyUserWithoutAcceptedMembership() {
        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectPermissionService.checkReadPermission(PROJECT_ID, user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have access to this project");

        verifyAcceptedMembershipQuery();
    }

    @Test
    void checkEditFieldsPermission_shouldDenyWhenMembershipIsNotAccepted() {
        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                projectPermissionService.checkEditFieldsPermission(PROJECT_ID, user)
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have access to this project");
    }

    private void mockAcceptedMembership(ProjectMemberRole role) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.setUser(user);
        projectMember.setRole(role);
        projectMember.setStatus(ProjectMemberStatus.ACCEPTED);

        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED))
                .thenReturn(Optional.of(projectMember));
    }

    private void verifyAcceptedMembershipQuery() {
        verify(projectMemberRepository).findByProjectIdAndUserIdAndStatus(
                PROJECT_ID, user.getId(), ProjectMemberStatus.ACCEPTED
        );
    }
}
