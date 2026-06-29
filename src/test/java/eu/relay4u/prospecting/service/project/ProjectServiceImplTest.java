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
import eu.relay4u.prospecting.repository.ProjectRepository;
import eu.relay4u.prospecting.repository.ProspectRecordRepository;
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
    void getProjects_returnsListWithCounts() {
        when(projectRepository.findAllByOwner(user)).thenReturn(List.of(project));
        when(projectFieldRepository.countByProject(project)).thenReturn(5L);
        when(prospectRecordRepository.countByProject(project)).thenReturn(3L);

        List<ProjectSummaryDto> result = projectService.getProjects(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fieldCount()).isEqualTo(5L);
        assertThat(result.get(0).recordCount()).isEqualTo(3L);
        assertThat(result.get(0).name()).isEqualTo("Test Project");
    }

    @Test
    void createProject_createsProjectWith5DefaultFields() {
        when(projectRepository.save(any())).thenReturn(project);

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
    void getProject_returnsProjectDtoWithFields() {
        ProjectField field = TestDataFactory.aField(project);
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(field));

        ProjectDto result = projectService.getProject(1L, user);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.fields()).hasSize(1);
        assertThat(result.fields().get(0).key()).isEqualTo("contact_name");
    }

    @Test
    void updateProject_updatesNameAndDescription() {
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of());

        projectService.updateProject(1L, new UpdateProjectRequest("New Name", "New Desc"), user);

        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("New Desc");
        verify(projectRepository).save(project);
    }

    @Test
    void updateProject_skipsNull_updatesOnlyName() {
        project.setDescription("Original Desc");
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of());

        projectService.updateProject(1L, new UpdateProjectRequest("New Name", null), user);

        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("Original Desc");
    }

    @Test
    void deleteProject_callsRepositoryDelete() {
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));

        projectService.deleteProject(1L, user);

        verify(projectRepository).delete(project);
    }

    @Test
    void addField_savesAndReturnsFieldDto() {
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.existsByProjectAndKey(project, "new_key")).thenReturn(false);

        FieldDefinitionDto result = projectService.addField(1L, TestDataFactory.createFieldRequest(), user);

        verify(projectFieldRepository).save(any(ProjectField.class));
        assertThat(result.key()).isEqualTo("new_key");
        assertThat(result.label()).isEqualTo("New Label");
    }

    @Test
    void deleteField_deletesFieldBelongingToProject() {
        ProjectField field = TestDataFactory.aField(project);
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findById(field.getId())).thenReturn(Optional.of(field));

        projectService.deleteField(1L, field.getId(), user);

        verify(projectFieldRepository).delete(field);
    }

    @Test
    void reorderFields_assignsNewOrderByPosition() {
        ProjectField f1 = TestDataFactory.aField(project);
        ProjectField f2 = TestDataFactory.aField(project);
        f1.setKey("first");
        f2.setKey("second");
        List<UUID> newOrder = List.of(f2.getId(), f1.getId());

        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(f1, f2));

        projectService.reorderFields(1L, new ReorderFieldsRequest(newOrder), user);

        assertThat(f2.getFieldOrder()).isZero();
        assertThat(f1.getFieldOrder()).isEqualTo(1);
    }

    // --- Sad path ---

    @Test
    void getProject_throwsProjectNotFoundException_whenNotOwned() {
        when(projectRepository.findByIdAndOwner(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(99L, user))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void updateProject_throwsProjectNotFoundException() {
        when(projectRepository.findByIdAndOwner(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject(99L, new UpdateProjectRequest("x", null), user))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void deleteProject_throwsProjectNotFoundException() {
        when(projectRepository.findByIdAndOwner(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99L, user))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void addField_throwsFieldKeyConflictException_whenKeyExists() {
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.existsByProjectAndKey(project, "new_key")).thenReturn(true);

        assertThatThrownBy(() -> projectService.addField(1L, TestDataFactory.createFieldRequest(), user))
                .isInstanceOf(FieldKeyConflictException.class);
    }

    @Test
    void deleteField_throwsProjectNotFoundException_whenFieldBelongsToDifferentProject() {
        Project otherProject = TestDataFactory.aProject(user);
        otherProject.setId(99L);
        ProjectField field = TestDataFactory.aField(otherProject);

        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findById(field.getId())).thenReturn(Optional.of(field));

        assertThatThrownBy(() -> projectService.deleteField(1L, field.getId(), user))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void reorderFields_throwsIllegalArgumentException_whenIdsMismatch() {
        ProjectField f1 = TestDataFactory.aField(project);
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(f1));

        UUID wrongId = UUID.randomUUID();
        assertThatThrownBy(() -> projectService.reorderFields(1L, new ReorderFieldsRequest(List.of(wrongId)), user))
                .isInstanceOf(IllegalArgumentException.class);
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
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of());

        projectService.updateProject(1L, new UpdateProjectRequest(null, null), user);

        assertThat(project.getName()).isEqualTo("Original Name");
        assertThat(project.getDescription()).isEqualTo("Original Desc");
    }

    @Test
    void reorderFields_withSingleField_setsOrderToZero() {
        ProjectField field = TestDataFactory.aField(project);
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(field));

        projectService.reorderFields(1L, new ReorderFieldsRequest(List.of(field.getId())), user);

        assertThat(field.getFieldOrder()).isZero();
    }

    @ParameterizedTest
    @EnumSource(FieldType.class)
    void addField_withAllFieldTypes_savesSuccessfully(FieldType type) {
        when(projectRepository.findByIdAndOwner(1L, user)).thenReturn(Optional.of(project));
        when(projectFieldRepository.existsByProjectAndKey(any(), any())).thenReturn(false);

        FieldDefinitionDto result = projectService.addField(1L,
                new CreateFieldRequest("key_" + type.name().toLowerCase(), "Label", type, false, 0), user);

        assertThat(result.type()).isEqualTo(type);
    }

    @Test
    void getProjects_returnsEmptyList_whenUserHasNoProjects() {
        when(projectRepository.findAllByOwner(user)).thenReturn(List.of());

        List<ProjectSummaryDto> result = projectService.getProjects(user);

        assertThat(result).isEmpty();
    }
}
