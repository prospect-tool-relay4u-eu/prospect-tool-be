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
import eu.relay4u.prospecting.model.FieldType;
import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectField;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.ProjectFieldRepository;
import eu.relay4u.prospecting.repository.ProjectRepository;
import eu.relay4u.prospecting.repository.ProspectRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectFieldRepository projectFieldRepository;
    private final ProspectRecordRepository prospectRecordRepository;

    @Override
    public List<ProjectSummaryDto> getProjects(User user) {
        return projectRepository.findAllByOwner(user).stream()
                .map(p -> new ProjectSummaryDto(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        projectFieldRepository.countByProject(p),
                        prospectRecordRepository.countByProject(p),
                        p.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, User user) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setOwner(user);
        projectRepository.save(project);

        List<ProjectField> fields = createDefaultFields(project);
        projectFieldRepository.saveAll(fields);

        return toProjectDto(project, fields);
    }

    @Override
    public ProjectDto getProject(Long id, User user) {
        Project project = findOwnedProject(id, user);
        List<ProjectField> fields = projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project);
        return toProjectDto(project, fields);
    }

    @Override
    @Transactional
    public ProjectDto updateProject(Long id, UpdateProjectRequest request, User user) {
        Project project = findOwnedProject(id, user);
        if (request.name() != null) project.setName(request.name());
        if (request.description() != null) project.setDescription(request.description());
        projectRepository.save(project);
        List<ProjectField> fields = projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project);
        return toProjectDto(project, fields);
    }

    @Override
    @Transactional
    public void deleteProject(Long id, User user) {
        Project project = findOwnedProject(id, user);
        prospectRecordRepository.softDeleteAllByProject(project);
        projectFieldRepository.deleteAllByProject(project);
        projectRepository.delete(project);
    }

    @Override
    @Transactional
    public FieldDefinitionDto addField(Long projectId, CreateFieldRequest request, User user) {
        Project project = findOwnedProject(projectId, user);
        if (projectFieldRepository.existsByProjectAndKey(project, request.key())) {
            throw new FieldKeyConflictException(request.key());
        }
        ProjectField field = new ProjectField();
        field.setProject(project);
        field.setKey(request.key());
        field.setLabel(request.label());
        field.setType(request.type());
        field.setRequired(request.required());
        field.setFieldOrder(request.order());
        projectFieldRepository.save(field);
        return toFieldDto(field);
    }

    @Override
    @Transactional
    public void deleteField(Long projectId, UUID fieldId, User user) {
        Project project = findOwnedProject(projectId, user);
        ProjectField field = projectFieldRepository.findById(fieldId)
                .filter(f -> f.getProject().getId().equals(project.getId()))
                .orElseThrow(ProjectNotFoundException::new);
        projectFieldRepository.delete(field);
    }

    @Override
    @Transactional
    public List<FieldDefinitionDto> reorderFields(Long projectId, ReorderFieldsRequest request, User user) {
        Project project = findOwnedProject(projectId, user);
        List<ProjectField> existing = projectFieldRepository.findAllByProjectOrderByFieldOrderAsc(project);

        Set<UUID> existingIds = existing.stream().map(ProjectField::getId).collect(Collectors.toSet());
        Set<UUID> requestIds = Set.copyOf(request.fieldIds());
        if (!existingIds.equals(requestIds)) {
            throw new IllegalArgumentException("Field IDs must match exactly all fields in the project");
        }

        Map<UUID, ProjectField> fieldMap = existing.stream()
                .collect(Collectors.toMap(ProjectField::getId, Function.identity()));

        List<ProjectField> reordered = new ArrayList<>();
        List<UUID> fieldIds = request.fieldIds();
        for (int i = 0; i < fieldIds.size(); i++) {
            ProjectField field = fieldMap.get(fieldIds.get(i));
            field.setFieldOrder(i);
            reordered.add(field);
        }
        projectFieldRepository.saveAll(reordered);
        return reordered.stream().map(this::toFieldDto).toList();
    }

    private Project findOwnedProject(Long id, User user) {
        return projectRepository.findByIdAndOwner(id, user)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private List<ProjectField> createDefaultFields(Project project) {
        return List.of(
                buildField(project, "contact_name", "Full Name", FieldType.STRING, true, 0),
                buildField(project, "company", "Company", FieldType.STRING, false, 1),
                buildField(project, "message_sent", "Message Sent", FieldType.STRING, false, 2),
                buildField(project, "replied", "Replied?", FieldType.BOOLEAN, false, 3),
                buildField(project, "reply_content", "Reply Content", FieldType.STRING, false, 4)
        );
    }

    private ProjectField buildField(Project project, String key, String label, FieldType type, boolean required, int order) {
        ProjectField f = new ProjectField();
        f.setProject(project);
        f.setKey(key);
        f.setLabel(label);
        f.setType(type);
        f.setRequired(required);
        f.setFieldOrder(order);
        return f;
    }

    private ProjectDto toProjectDto(Project project, List<ProjectField> fields) {
        return new ProjectDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                fields.stream().map(this::toFieldDto).toList()
        );
    }

    private FieldDefinitionDto toFieldDto(ProjectField f) {
        return new FieldDefinitionDto(f.getId(), f.getKey(), f.getLabel(), f.getType(), f.isRequired(), f.getFieldOrder());
    }
}
