package eu.relay4u.prospecting.service.project;

import eu.relay4u.prospecting.dto.field.CreateFieldRequest;
import eu.relay4u.prospecting.dto.field.FieldDefinitionDto;
import eu.relay4u.prospecting.dto.field.ReorderFieldsRequest;
import eu.relay4u.prospecting.dto.project.CreateProjectRequest;
import eu.relay4u.prospecting.dto.project.ProjectDto;
import eu.relay4u.prospecting.dto.project.ProjectSummaryDto;
import eu.relay4u.prospecting.dto.project.UpdateProjectRequest;
import eu.relay4u.prospecting.exception.FieldKeyConflictException;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.model.*;
import eu.relay4u.prospecting.repository.ProjectFieldRepository;
import eu.relay4u.prospecting.repository.ProjectMemberRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;
import eu.relay4u.prospecting.repository.ProspectRecordRepository;
import eu.relay4u.prospecting.service.projectpermission.ProjectPermissionService;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock ProjectRepository projectRepository;
    @Mock ProjectFieldRepository projectFieldRepository;
    @Mock ProspectRecordRepository prospectRecordRepository;
    @Mock ProjectMemberRepository projectMemberRepository;
    @Mock ProjectPermissionService projectPermissionService;

    @InjectMocks ProjectServiceImpl projectService;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.aUser();
        project = TestDataFactory.aProject(user);
    }

    // --- Happy path ---

    @Test
    void getProjects_returnsPageWithCounts() {
        Pageable pageable = PageRequest.of(0, 10);
        ProjectMember membership = new ProjectMember().setProject(project).setUser(user)
                .setRole(ProjectMemberRole.OWNER).setStatus(ProjectMemberStatus.ACCEPTED);

        when(projectMemberRepository.findAllByUserAndStatus(
                user, ProjectMemberStatus.ACCEPTED, pageable))
                .thenReturn(new PageImpl<>(List.of(membership), pageable, 1));
        when(projectFieldRepository.countByProject(project)).thenReturn(5L);
        when(prospectRecordRepository.countByProject(project)).thenReturn(3L);

        Page<ProjectSummaryDto> result = projectService.getProjects(user, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).fieldCount()).isEqualTo(5L);
        assertThat(result.getContent().get(0).recordCount()).isEqualTo(3L);
        assertThat(result.getContent().get(0).name()).isEqualTo("Test Project");
    }

    @Test
    void createProject_createsProjectWith5DefaultFields() {
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        projectService.createProject(TestDataFactory.createProjectRequest(), user);

        ArgumentCaptor<List<ProjectField>> captor = ArgumentCaptor.captor();
        verify(projectFieldRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(5);
    }

    @Test
    void createProject_defaultFieldsHaveCorrectKeys() {
        when(projectRepository.save(any())).thenReturn(project);

        projectService.createProject(TestDataFactory.createProjectRequest(), user);

        ArgumentCaptor<List<ProjectField>> captor = ArgumentCaptor.captor();
        verify(projectFieldRepository).saveAll(captor.capture());
        List<String> keys = captor.getValue().stream().map(ProjectField::getKey).toList();
        assertThat(keys).containsExactly("contact_name", "company", "message_sent", "replied", "reply_content");
    }

    @Test
    void createProject_createsAcceptedOwnerMembership() {
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        projectService.createProject(TestDataFactory.createProjectRequest(), user);

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);

        verify(projectMemberRepository).save(memberCaptor.capture());

        ProjectMember savedMember = memberCaptor.getValue();

        assertThat(savedMember.getProject()).isNotNull();
        assertThat(savedMember.getUser()).isEqualTo(user);
        assertThat(savedMember.getRole()).isEqualTo(ProjectMemberRole.OWNER);
        assertThat(savedMember.getStatus()).isEqualTo(ProjectMemberStatus.ACCEPTED);
        assertThat(savedMember.getInvitedEmail()).isNull();
    }

    @Test
    void getProject_returnsProjectDtoWithFields() {
        ProjectField field = TestDataFactory.aField(project);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(field));

        ProjectDto result = projectService.getProject(1L, user);

        verify(projectPermissionService).checkReadPermission(1L, user);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.fields()).hasSize(1);
        assertThat(result.fields().get(0).key()).isEqualTo("contact_name");
    }

    @Test
    void updateProject_updatesNameAndDescription() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of());

        projectService.updateProject(1L, new UpdateProjectRequest("New Name", "New Desc"), user);

        verify(projectPermissionService).checkOwnerPermission(1L, user);
        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("New Desc");
        verify(projectRepository).save(project);
    }

    @Test
    void updateProject_skipsNull_updatesOnlyName() {
        project.setDescription("Original Desc");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of());

        projectService.updateProject(1L, new UpdateProjectRequest("New Name", null), user);

        verify(projectPermissionService).checkOwnerPermission(1L, user);
        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("Original Desc");
    }

    @Test
    void deleteProject_deletesChildrenThenProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        projectService.deleteProject(1L, user);

        verify(projectPermissionService).checkOwnerPermission(1L, user);
        verify(prospectRecordRepository).softDeleteAllByProject(project);
        verify(projectFieldRepository).deleteAllByProject(project);
        verify(projectRepository).delete(project);
    }

    @Test
    void addField_savesAndReturnsFieldDto() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.existsByProjectAndKey(project, "new_key")).thenReturn(false);

        FieldDefinitionDto result = projectService.addField(1L, TestDataFactory.createFieldRequest(), user);

        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        verify(projectFieldRepository).save(any(ProjectField.class));
        assertThat(result.key()).isEqualTo("new_key");
        assertThat(result.label()).isEqualTo("New Label");
    }

    @Test
    void deleteField_deletesFieldBelongingToProject() {
        ProjectField field = TestDataFactory.aField(project);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findById(field.getId())).thenReturn(Optional.of(field));

        projectService.deleteField(1L, field.getId(), user);

        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        verify(projectFieldRepository).delete(field);
    }

    @Test
    void reorderFields_assignsNewOrderByPosition() {
        ProjectField f1 = TestDataFactory.aField(project);
        ProjectField f2 = TestDataFactory.aField(project);
        f1.setKey("first");
        f2.setKey("second");
        List<UUID> newOrder = List.of(f2.getId(), f1.getId());

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(f1, f2));

        projectService.reorderFields(1L, new ReorderFieldsRequest(newOrder), user);

        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        assertThat(f2.getFieldOrder()).isZero();
        assertThat(f1.getFieldOrder()).isEqualTo(1);
    }

    @Test
    void countRecords_returnsRecordCount() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(prospectRecordRepository.countAllByProjectAndIsDeletedFalse(project))
                .thenReturn(7L);

        Long result = projectService.countRecords(1L, user);

        verify(projectPermissionService).checkReadPermission(1L, user);
        assertThat(result).isEqualTo(7L);
    }

    // --- Sad path ---

    @Test
    void getProject_throwsProjectNotFoundException_whenNotOwned() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.getProject(99L, user))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectPermissionService).checkReadPermission(99L, user);
    }

    @Test
    void updateProject_throwsProjectNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject(99L, new UpdateProjectRequest("x", null), user))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectPermissionService).checkOwnerPermission(99L, user);
    }

    @Test
    void deleteProject_throwsProjectNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99L, user))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectPermissionService).checkOwnerPermission(99L, user);
    }

    @Test
    void addField_throwsFieldKeyConflictException_whenKeyExists() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.existsByProjectAndKey(project, "new_key")).thenReturn(true);

        assertThatThrownBy(() -> projectService.addField(1L, TestDataFactory.createFieldRequest(), user))
                .isInstanceOf(FieldKeyConflictException.class);
        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        verify(projectFieldRepository, never()).save(any(ProjectField.class));
    }

    @Test
    void addField_doesNotSaveField_whenPermissionIsDenied() {
        doThrow(new AccessDeniedException("Edit fields permission denied"))
                .when(projectPermissionService)
                .checkEditFieldsPermission(1L, user);

        assertThatThrownBy(() -> projectService.addField(1L, TestDataFactory.createFieldRequest(), user))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(anyLong());
        verify(projectFieldRepository, never()).save(any(ProjectField.class));
    }

    @Test
    void deleteField_throwsProjectNotFoundException_whenFieldBelongsToDifferentProject() {
        Project otherProject = TestDataFactory.aProject(user);
        otherProject.setId(99L);
        ProjectField field = TestDataFactory.aField(otherProject);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findById(field.getId())).thenReturn(Optional.of(field));

        assertThatThrownBy(() -> projectService.deleteField(1L, field.getId(), user))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        verify(projectFieldRepository, never()).delete(any(ProjectField.class));
    }

    @Test
    void reorderFields_throwsIllegalArgumentException_whenIdsMismatch() {
        ProjectField f1 = TestDataFactory.aField(project);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(f1));

        UUID wrongId = UUID.randomUUID();
        assertThatThrownBy(() -> projectService.reorderFields(1L, new ReorderFieldsRequest(List.of(wrongId)), user))
                .isInstanceOf(IllegalArgumentException.class);
        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        verify(projectFieldRepository, never()).saveAll(any());
    }

    @Test
    void countRecords_throwsProjectNotFoundException_whenProjectDoesNotExist() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.countRecords(99L, user))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectPermissionService).checkReadPermission(99L, user);
        verify(prospectRecordRepository, never())
                .countAllByProjectAndIsDeletedFalse(any(Project.class));
    }

    // --- Edge cases ---

    @Test
    void createProject_withNullDescription_createsSuccessfully() {
        when(projectRepository.save(any())).thenReturn(project);

        assertThatCode(() -> projectService.createProject(new CreateProjectRequest("Name", null), user))
                .doesNotThrowAnyException();
    }

    @Test
    void updateProject_withBothNullFields_makesNoChanges() {
        project.setName("Original Name");
        project.setDescription("Original Desc");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of());

        projectService.updateProject(1L, new UpdateProjectRequest(null, null), user);

        verify(projectPermissionService).checkOwnerPermission(1L, user);
        assertThat(project.getName()).isEqualTo("Original Name");
        assertThat(project.getDescription()).isEqualTo("Original Desc");
    }

    @Test
    void reorderFields_withSingleField_setsOrderToZero() {
        ProjectField field = TestDataFactory.aField(project);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(field));

        projectService.reorderFields(1L, new ReorderFieldsRequest(List.of(field.getId())), user);

        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        assertThat(field.getFieldOrder()).isZero();
    }

    @ParameterizedTest
    @EnumSource(FieldType.class)
    void addField_withAllFieldTypes_savesSuccessfully(FieldType type) {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectFieldRepository.existsByProjectAndKey(any(), any())).thenReturn(false);

        FieldDefinitionDto result = projectService.addField(1L,
                new CreateFieldRequest("key_" + type.name().toLowerCase(), "Label", type, false, 0), user);

        verify(projectPermissionService).checkEditFieldsPermission(1L, user);
        assertThat(result.type()).isEqualTo(type);
    }

    @Test
    void getProjects_returnsEmptyList_whenUserHasNoProjects() {
        Pageable pageable = PageRequest.of(0, 10);

        when(projectMemberRepository.findAllByUserAndStatus(
                user, ProjectMemberStatus.ACCEPTED, pageable)).thenReturn(Page.empty(pageable));

        Page<ProjectSummaryDto> result = projectService.getProjects(user, pageable);

        assertThat(result).isEmpty();
    }
}
