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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final ProjectRepository projectRepository;
    private final ProspectRecordRepository prospectRecordRepository;
    private final ProjectFieldRepository projectFieldRepository;
    private final ProjectPermissionService projectPermissionService;

    @Override
    public Page<ProspectRecordDto> getRecords(Long projectId, User user, Pageable pageable) {
        projectPermissionService.checkReadPermission(projectId, user);
        Project project = findProjectById(projectId);
        return prospectRecordRepository.findAllByProjectOrderByCreatedAtAsc(project, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public ProspectRecordDto createRecord(Long projectId, User user) {
        projectPermissionService.checkEditRecordsPermission(projectId, user);
        Project project = findProjectById(projectId);
        ProspectRecord record = new ProspectRecord();
        record.setProject(project);
        prospectRecordRepository.save(record);
        return toDto(record);
    }

    @Override
    @Transactional
    public ProspectRecordDto updateRecord(UUID recordId, UpdateRecordRequest request, User user) {
        ProspectRecord record = findRecordById(recordId);
        Long projectId = record.getProject().getId();
        projectPermissionService.checkEditRecordsPermission(projectId, user);
        validateFieldValues(record.getProject(), request.values());
        record.getValues().putAll(request.values());
        prospectRecordRepository.save(record);
        return toDto(record);
    }

    private void validateFieldValues(Project project, Map<String, Object> values) {
        List<ProjectField> fields = projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project);
        Map<String, FieldType> typeByKey = fields.stream()
                .collect(java.util.stream.Collectors.toMap(ProjectField::getKey, ProjectField::getType));

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            FieldType type = typeByKey.get(entry.getKey());
            if (type == null) {
                continue;
            }
            Object value = entry.getValue();
            if (!isValidValue(type, value)) {
                throw new InvalidFieldValueException(entry.getKey(),
                        "Value for field '" + entry.getKey() + "' must be of type " + type + ".");
            }
        }
    }

    private boolean isValidValue(FieldType type, Object value) {
        if (value == null) {
            return true;
        }
        return switch (type) {
            case STRING -> value instanceof String;
            case BOOLEAN -> value instanceof Boolean;
            case INTEGER -> value instanceof Integer || value instanceof Long
                    || (value instanceof Double d && !Double.isInfinite(d) && d == Math.floor(d));
            case NUMBER -> value instanceof Number;
        };
    }

    @Override
    @Transactional
    public void deleteRecord(UUID recordId, User user) {
        ProspectRecord record = findRecordById(recordId);
        projectPermissionService.checkEditRecordsPermission(record.getProject().getId(), user);
        prospectRecordRepository.delete(record);
    }

    @Override
    @Transactional
    public void clearAllRecords(Long projectId, User user) {
        projectPermissionService.checkEditRecordsPermission(projectId, user);
        Project project = findProjectById(projectId);
        prospectRecordRepository.softDeleteAllByProject(project);
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private ProspectRecord findRecordById(UUID recordId) {
        return prospectRecordRepository.findById(recordId)
                .orElseThrow(RecordNotFoundException::new);
    }

    private ProspectRecordDto toDto(ProspectRecord record) {
        return new ProspectRecordDto(
                record.getId(),
                record.getProject().getId(),
                record.getValues(),
                record.getCreatedAt()
        );
    }
}
