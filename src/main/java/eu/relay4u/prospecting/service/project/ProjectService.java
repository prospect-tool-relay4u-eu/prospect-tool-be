package eu.relay4u.prospecting.service.project;

import eu.relay4u.prospecting.dto.field.CreateFieldRequest;
import eu.relay4u.prospecting.dto.field.FieldDefinitionDto;
import eu.relay4u.prospecting.dto.field.ReorderFieldsRequest;
import eu.relay4u.prospecting.dto.project.CreateProjectRequest;
import eu.relay4u.prospecting.dto.project.ProjectDto;
import eu.relay4u.prospecting.dto.project.ProjectSummaryDto;
import eu.relay4u.prospecting.dto.project.UpdateProjectRequest;
import eu.relay4u.prospecting.model.User;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    List<ProjectSummaryDto> getProjects(User user);
    ProjectDto createProject(CreateProjectRequest request, User user);
    ProjectDto getProject(Long id, User user);
    ProjectDto updateProject(Long id, UpdateProjectRequest request, User user);
    void deleteProject(Long id, User user);
    FieldDefinitionDto addField(Long projectId, CreateFieldRequest request, User user);
    void deleteField(Long projectId, UUID fieldId, User user);
    List<FieldDefinitionDto> reorderFields(Long projectId, ReorderFieldsRequest request, User user);
}
