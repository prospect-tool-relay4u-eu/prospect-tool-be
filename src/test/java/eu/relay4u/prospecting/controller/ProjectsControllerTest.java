package eu.relay4u.prospecting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.relay4u.prospecting.dto.field.CreateFieldRequest;
import eu.relay4u.prospecting.dto.field.FieldDefinitionDto;
import eu.relay4u.prospecting.dto.field.ReorderFieldsRequest;
import eu.relay4u.prospecting.dto.project.CreateProjectRequest;
import eu.relay4u.prospecting.dto.project.ProjectDto;
import eu.relay4u.prospecting.dto.project.ProjectSummaryDto;
import eu.relay4u.prospecting.dto.project.UpdateProjectRequest;
import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.exception.FieldKeyConflictException;
import eu.relay4u.prospecting.exception.GlobalExceptionHandler;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.model.FieldType;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.service.project.ProjectService;
import eu.relay4u.prospecting.service.record.RecordService;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProjectsControllerTest {

    @Mock ProjectService projectService;
    @Mock RecordService recordService;
    @InjectMocks ProjectsController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    User mockUser;
    ProjectSummaryDto summaryDto;
    ProjectDto projectDto;
    FieldDefinitionDto fieldDto;
    ProspectRecordDto recordDto;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();

        mockUser = TestDataFactory.aUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities())
        );

        fieldDto = new FieldDefinitionDto(UUID.randomUUID(), "key", "Label", FieldType.STRING, false, 0);
        summaryDto = new ProjectSummaryDto(1L, "Project", "Desc", 5L, 3L, LocalDateTime.now());
        projectDto = new ProjectDto(1L, "Project", "Desc", LocalDateTime.now(), List.of(fieldDto));
        recordDto = new ProspectRecordDto(UUID.randomUUID(), 1L, Map.of(), LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- GET /api/projects ---

    @Test
    void getProjectsList_returns200() throws Exception {
        when(projectService.getProjects(any())).thenReturn(List.of(summaryDto));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Project"))
                .andExpect(jsonPath("$[0].fieldCount").value(5));
    }

    // --- POST /api/projects ---

    @Test
    void createProject_returns201() throws Exception {
        when(projectService.createProject(any(), any())).thenReturn(projectDto);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("Project", "Desc"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createProject_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("", "Desc"))))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/projects/{id} ---

    @Test
    void getProject_returns200() throws Exception {
        when(projectService.getProject(eq(1L), any())).thenReturn(projectDto);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project"));
    }

    @Test
    void getProject_returns404_whenNotFound() throws Exception {
        when(projectService.getProject(eq(99L), any())).thenThrow(new ProjectNotFoundException());

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/projects/{id} ---

    @Test
    void updateProject_returns200() throws Exception {
        when(projectService.updateProject(eq(1L), any(), any())).thenReturn(projectDto);

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProjectRequest("New Name", null))))
                .andExpect(status().isOk());
    }

    @Test
    void updateProject_returns404_whenNotFound() throws Exception {
        when(projectService.updateProject(eq(99L), any(), any())).thenThrow(new ProjectNotFoundException());

        mockMvc.perform(put("/api/projects/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProjectRequest("x", null))))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/projects/{id} ---

    @Test
    void deleteProject_returns204() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(eq(1L), any());
    }

    @Test
    void deleteProject_returns404_whenNotFound() throws Exception {
        doThrow(new ProjectNotFoundException()).when(projectService).deleteProject(eq(99L), any());

        mockMvc.perform(delete("/api/projects/99"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/projects/{id}/fields ---

    @Test
    void addField_returns201() throws Exception {
        when(projectService.addField(eq(1L), any(), any())).thenReturn(fieldDto);

        mockMvc.perform(post("/api/projects/1/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFieldRequest("key", "Label", FieldType.STRING, false, 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("key"));
    }

    @Test
    void addField_returns409_whenKeyConflict() throws Exception {
        when(projectService.addField(eq(1L), any(), any())).thenThrow(new FieldKeyConflictException("key"));

        mockMvc.perform(post("/api/projects/1/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFieldRequest("key", "Label", FieldType.STRING, false, 0))))
                .andExpect(status().isConflict());
    }

    @Test
    void addField_returns400_whenKeyBlank() throws Exception {
        mockMvc.perform(post("/api/projects/1/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFieldRequest("", "Label", FieldType.STRING, false, 0))))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/projects/{id}/fields/{fieldId} ---

    @Test
    void removeField_returns204() throws Exception {
        UUID fieldId = UUID.randomUUID();

        mockMvc.perform(delete("/api/projects/1/fields/" + fieldId))
                .andExpect(status().isNoContent());

        verify(projectService).deleteField(eq(1L), eq(fieldId), any());
    }

    // --- PUT /api/projects/{id}/fields/order ---

    @Test
    void changeFieldsOrder_returns200() throws Exception {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(projectService.reorderFields(eq(1L), any(), any())).thenReturn(List.of(fieldDto));

        mockMvc.perform(put("/api/projects/1/fields/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderFieldsRequest(ids))))
                .andExpect(status().isOk());
    }

    @Test
    void changeFieldsOrder_returns400_whenIllegalArgument() throws Exception {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(projectService.reorderFields(eq(1L), any(), any()))
                .thenThrow(new IllegalArgumentException("mismatch"));

        mockMvc.perform(put("/api/projects/1/fields/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReorderFieldsRequest(ids))))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/projects/{id}/records ---

    @Test
    void getRecords_returns200() throws Exception {
        when(recordService.getRecords(eq(1L), any())).thenReturn(List.of(recordDto));

        mockMvc.perform(get("/api/projects/1/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1));
    }

    // --- POST /api/projects/{id}/records ---

    @Test
    void createRecord_returns201() throws Exception {
        when(recordService.createRecord(eq(1L), any())).thenReturn(recordDto);

        mockMvc.perform(post("/api/projects/1/records"))
                .andExpect(status().isCreated());
    }

    // --- DELETE /api/projects/{id}/records ---

    @Test
    void clearAllRecords_returns204() throws Exception {
        mockMvc.perform(delete("/api/projects/1/records"))
                .andExpect(status().isNoContent());

        verify(recordService).clearAllRecords(eq(1L), any());
    }
}
