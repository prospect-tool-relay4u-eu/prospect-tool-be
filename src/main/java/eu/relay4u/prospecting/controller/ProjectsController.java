package eu.relay4u.prospecting.controller;

import eu.relay4u.prospecting.dto.field.CreateFieldRequest;
import eu.relay4u.prospecting.dto.field.FieldDefinitionDto;
import eu.relay4u.prospecting.dto.field.ReorderFieldsRequest;
import eu.relay4u.prospecting.dto.project.CreateProjectRequest;
import eu.relay4u.prospecting.dto.project.ProjectDto;
import eu.relay4u.prospecting.dto.project.ProjectSummaryDto;
import eu.relay4u.prospecting.dto.project.UpdateProjectRequest;
import eu.relay4u.prospecting.dto.project_member.InviteMemberToProjectDto;
import eu.relay4u.prospecting.dto.project_member.ProjectMemberDto;
import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.service.project.ProjectService;
import eu.relay4u.prospecting.service.project_member.ProjectMemberInvitationService;
import eu.relay4u.prospecting.service.record.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.member;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectsController {

    private final ProjectService projectService;
    private final RecordService recordService;
    private final ProjectMemberInvitationService projectMemberInvitationService;

    @GetMapping
    public Page<ProjectSummaryDto> getProjectsList(@AuthenticationPrincipal User user,
                                                   @ParameterObject Pageable pageable) {
        return projectService.getProjects(user,pageable);
    }

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@RequestBody @Valid CreateProjectRequest request,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request, user));
    }

    @GetMapping("/{id}")
    public ProjectDto projectDetails(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return projectService.getProject(id, user);
    }

    @PutMapping("/{id}")
    public ProjectDto changeProjectDetails(@PathVariable Long id,
                                           @RequestBody @Valid UpdateProjectRequest request,
                                           @AuthenticationPrincipal User user) {
        return projectService.updateProject(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, @AuthenticationPrincipal User user) {
        projectService.deleteProject(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fields")
    public ResponseEntity<FieldDefinitionDto> addFieldsToProject(@PathVariable Long id,
                                                                  @RequestBody @Valid CreateFieldRequest request,
                                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addField(id, request, user));
    }

    @DeleteMapping("/{id}/fields/{fieldId}")
    public ResponseEntity<Void> removeFieldsFromProject(@PathVariable Long id,
                                                         @PathVariable UUID fieldId,
                                                         @AuthenticationPrincipal User user) {
        projectService.deleteField(id, fieldId, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/fields/order")
    public List<FieldDefinitionDto> changeFieldsOrder(@PathVariable Long id,
                                                       @RequestBody @Valid ReorderFieldsRequest request,
                                                       @AuthenticationPrincipal User user) {
        return projectService.reorderFields(id, request, user);
    }

    @GetMapping("/{id}/records")
    public Page<ProspectRecordDto> recordsListInProject(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user,
                                                        @ParameterObject Pageable pageable) {
        return recordService.getRecords(id, user, pageable);
    }

    @PostMapping("/{id}/records")
    public ResponseEntity<ProspectRecordDto> createEmptyRecord(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.createRecord(id, user));
    }

    @DeleteMapping("/{id}/records")
    public ResponseEntity<Void> clearAllRecords(@PathVariable Long id, @AuthenticationPrincipal User user) {
        recordService.clearAllRecords(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<ProjectMemberDto> createMemberInvitation(
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberToProjectDto request,
            @AuthenticationPrincipal User user
    ) {
        ProjectMember member =
                projectMemberInvitationService.inviteMemberToProject(id, request, user);

        ProjectMemberDto dto = new ProjectMemberDto(
                member.getId(),
                member.getInvitedEmail(),
                member.getRole(),
                member.getStatus()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
