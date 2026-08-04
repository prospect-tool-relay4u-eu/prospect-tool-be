package eu.relay4u.prospecting.service.record;

import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.dto.record.UpdateRecordRequest;
import eu.relay4u.prospecting.exception.InvalidFieldValueException;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.exception.RecordNotFoundException;
import eu.relay4u.prospecting.model.FieldType;
import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectField;
import eu.relay4u.prospecting.model.ProspectRecord;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.ProjectFieldRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;
import eu.relay4u.prospecting.repository.ProspectRecordRepository;
import eu.relay4u.prospecting.service.projectpermission.ProjectPermissionService;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    ProspectRecordRepository prospectRecordRepository;
    @Mock
    ProjectFieldRepository projectFieldRepository;
    @Mock
    ProjectPermissionService projectPermissionService;

    @InjectMocks
    RecordServiceImpl recordService;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.aUser();
        project = TestDataFactory.aProject(user);
    }

    // --- Happy path ---

    @Test
    void getRecords_returnsDtosInOrder() {
        Pageable pageable = PageRequest.of(0, 10);

        ProspectRecord r1 = TestDataFactory.aRecord(project);
        ProspectRecord r2 = TestDataFactory.aRecord(project);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(prospectRecordRepository.findAllByProjectOrderByCreatedAtAsc(project, pageable))
                .thenReturn(new PageImpl<>(List.of(r1, r2)));

        Page<ProspectRecordDto> result = recordService.getRecords(1L, user, pageable);

        verify(projectPermissionService).checkReadPermission(1L, user);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).projectId()).isEqualTo(1L);
    }

    @Test
    void createRecord_savesEmptyValuesMap() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(prospectRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProspectRecordDto result = recordService.createRecord(1L, user);

        verify(projectPermissionService).checkEditRecordsPermission(1L, user);
        assertThat(result.values()).isEmpty();
        verify(prospectRecordRepository).save(any(ProspectRecord.class));
    }

    @Test
    void updateRecord_mergesNewValuesIntoExisting() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        record.getValues().put("existing_key", "existing_value");
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(prospectRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> newValues = Map.of("new_key", "new_value");
        recordService.updateRecord(record.getId(), new UpdateRecordRequest(newValues), user);

        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
        assertThat(record.getValues()).containsEntry("existing_key", "existing_value");
        assertThat(record.getValues()).containsEntry("new_key", "new_value");
    }

    @Test
    void deleteRecord_callsRepositoryDelete() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));

        recordService.deleteRecord(record.getId(), user);

        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
        verify(prospectRecordRepository).delete(record);
    }

    @Test
    void clearAllRecords_callsSoftDeleteAll() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        recordService.clearAllRecords(1L, user);

        verify(projectPermissionService).checkEditRecordsPermission(1L, user);
        verify(prospectRecordRepository).softDeleteAllByProject(project);
    }

    // --- Sad path ---

    @Test
    void getRecords_throwsProjectNotFoundException_whenNotOwned() {
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.getRecords(99L, user, pageable))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void updateRecord_throwsAccessDeniedException_whenUserCannotEditRecords() {
        User otherUser = TestDataFactory.aUser();
        otherUser.setId(99L);
        Project otherProject = TestDataFactory.aProject(otherUser);
        ProspectRecord record = TestDataFactory.aRecord(otherProject);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));

        doThrow(new AccessDeniedException("Edit records permission denied"))
                .when(projectPermissionService)
                .checkEditRecordsPermission(otherProject.getId(), user);

        assertThatThrownBy(() -> recordService.updateRecord(
                record.getId(), new UpdateRecordRequest(Map.of()), user))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectPermissionService).checkEditRecordsPermission(otherProject.getId(), user);
        verify(prospectRecordRepository, never()).save(any(ProspectRecord.class));
    }

    @Test
    void deleteRecord_throwsProjectNotFoundException_whenRecordDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(prospectRecordRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.deleteRecord(unknownId, user))
                .isInstanceOf(RecordNotFoundException.class);

        verifyNoInteractions(projectPermissionService);
        verify(prospectRecordRepository, never()).delete(any(ProspectRecord.class));
    }

    @Test
    void clearAllRecords_throwsProjectNotFoundException_whenNotOwned() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.clearAllRecords(99L, user))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectPermissionService).checkEditRecordsPermission(99L, user);
        verify(prospectRecordRepository, never()).softDeleteAllByProject(any(Project.class));
    }

    // --- Edge cases ---

    @Test
    void updateRecord_withEmptyMap_preservesExistingValues() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        record.getValues().put("keep", "value");
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(prospectRecordRepository.save(any(ProspectRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        recordService.updateRecord(record.getId(), new UpdateRecordRequest(Map.of()), user);

        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
        assertThat(record.getValues()).containsEntry("keep", "value");
    }

    @Test
    void updateRecord_overwritesExistingKeyWithNewValue() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        record.getValues().put("name", "Old Name");
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(prospectRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> update = new HashMap<>();
        update.put("name", "New Name");
        recordService.updateRecord(record.getId(), new UpdateRecordRequest(update), user);

        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
        assertThat(record.getValues().get("name")).isEqualTo("New Name");
    }

    @Test
    void getRecords_returnsEmptyList_whenNoRecords() {
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(prospectRecordRepository.findAllByProjectOrderByCreatedAtAsc(project, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ProspectRecordDto> result = recordService.getRecords(1L, user, pageable);

        verify(projectPermissionService).checkReadPermission(1L, user);
        assertThat(result).isEmpty();
    }

    @Test
    void createRecord_multipleRecords_allHaveEmptyValues() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(prospectRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProspectRecordDto first = recordService.createRecord(1L, user);
        ProspectRecordDto second = recordService.createRecord(1L, user);

        verify(projectPermissionService,
                org.mockito.Mockito.times(2))
                .checkEditRecordsPermission(1L, user);
        assertThat(first.values()).isEmpty();
        assertThat(second.values()).isEmpty();
    }

    @Test
    void updateRecord_withNullValueInMap_isAccepted() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(prospectRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> valuesWithNull = new HashMap<>();
        valuesWithNull.put("nullable_field", null);
        assertThatCode(() -> recordService.updateRecord(record.getId(), new UpdateRecordRequest(valuesWithNull), user))
                .doesNotThrowAnyException();
        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
    }

    // --- Field type validation ---

    @Test
    void updateRecord_withMismatchedType_throwsInvalidFieldValueException() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        ProjectField ageField = TestDataFactory.aField(project);
        ageField.setKey("age");
        ageField.setType(FieldType.INTEGER);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(ageField));

        Map<String, Object> values = Map.of("age", "not-a-number");

        assertThatThrownBy(() -> recordService.updateRecord(record.getId(), new UpdateRecordRequest(values), user))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessageContaining("age");
        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
        verify(prospectRecordRepository, never()).save(any());
    }

    @Test
    void updateRecord_withMatchingType_isAccepted() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        ProjectField ageField = TestDataFactory.aField(project);
        ageField.setKey("age");
        ageField.setType(FieldType.INTEGER);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of(ageField));
        when(prospectRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> values = Map.of("age", 42);
        recordService.updateRecord(record.getId(), new UpdateRecordRequest(values), user);

        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
        assertThat(record.getValues()).containsEntry("age", 42);
    }

    @Test
    void updateRecord_withUnknownKey_isPassedThroughUnvalidated() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project)).thenReturn(List.of());
        when(prospectRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> values = Map.of("undeclared_key", "anything");
        recordService.updateRecord(record.getId(), new UpdateRecordRequest(values), user);

        verify(projectPermissionService).checkEditRecordsPermission(project.getId(), user);
        assertThat(record.getValues()).containsEntry("undeclared_key", "anything");
    }

    // --- Permissions ---

    @Test
    void getRecords_doesNotLoadProject_whenReadPermissionIsDenied() {
        Pageable pageable = PageRequest.of(0, 10);

        doThrow(new AccessDeniedException("Read permission denied"))
                .when(projectPermissionService)
                .checkReadPermission(1L, user);

        assertThatThrownBy(() -> recordService.getRecords(1L, user, pageable))
                .isInstanceOf(AccessDeniedException.class);
        verify(projectRepository, never()).findById(1L);
        verify(prospectRecordRepository, never())
                .findAllByProjectOrderByCreatedAtAsc(any(Project.class), any(Pageable.class));
    }

    @Test
    void createRecord_doesNotSave_whenEditPermissionIsDenied() {
        doThrow(new AccessDeniedException("Edit records permission denied"))
                .when(projectPermissionService).checkEditRecordsPermission(1L, user);

        assertThatThrownBy(() -> recordService.createRecord(1L, user))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectRepository, never()).findById(1L);
        verify(prospectRecordRepository, never()).save(any(ProspectRecord.class));
    }

    @Test
    void updateRecord_doesNotSave_whenEditPermissionIsDenied() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));

        doThrow(new AccessDeniedException("Edit records permission denied"))
                .when(projectPermissionService)
                .checkEditRecordsPermission(project.getId(), user);

        assertThatThrownBy(() -> recordService.updateRecord(
                record.getId(), new UpdateRecordRequest(Map.of()), user))
                .isInstanceOf(AccessDeniedException.class);
        verify(prospectRecordRepository, never()).save(any(ProspectRecord.class));
    }

    @Test
    void deleteRecord_doesNotDelete_whenEditPermissionIsDenied() {
        ProspectRecord record = TestDataFactory.aRecord(project);
        when(prospectRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));

        doThrow(new AccessDeniedException("Edit records permission denied"))
                .when(projectPermissionService)
                .checkEditRecordsPermission(project.getId(), user);

        assertThatThrownBy(() -> recordService.deleteRecord(record.getId(), user))
                .isInstanceOf(AccessDeniedException.class);
        verify(prospectRecordRepository, never()).delete(any(ProspectRecord.class));
    }
}
