# `ProjectsController` — `/api/projects`

All endpoints require a valid JWT (see [architecture.md](../architecture.md)) and are scoped to the authenticated user via `@AuthenticationPrincipal User user`. Controller methods are thin — they delegate immediately to `ProjectServiceImpl` or `RecordServiceImpl`. Project access is verified in the service layer through `ProjectPermissionService`.

## Endpoints

| Method | Path | Request | Response | Description |
|---|---|---|---|---|
| GET | `/` | – | `List<ProjectSummaryDto>` | List the current user's projects, each with field/record counts |
| POST | `/` | `CreateProjectRequest{name*, description}` | 201 `ProjectDto` | Create a project, seeded with 5 default fields |
| GET | `/{id}` | – | `ProjectDto` | Get project detail with fields ordered by `fieldOrder` |
| PUT | `/{id}` | `UpdateProjectRequest{name?, description?}` | `ProjectDto` | Partially update name/description |
| DELETE | `/{id}` | – | 204 | Delete a project (see note on cascade behavior below) |
| POST | `/{id}/fields` | `CreateFieldRequest{key*, label*, type*, required, order}` | 201 `FieldDefinitionDto` | Add a custom field definition |
| DELETE | `/{id}/fields/{fieldId}` | – | 204 | Remove a field definition |
| PUT | `/{id}/fields/order` | `ReorderFieldsRequest{fieldIds: List<UUID>}` | `List<FieldDefinitionDto>` | Reorder all fields in one call |
| GET | `/{id}/records` | – | `List<ProspectRecordDto>` | List records in a project, oldest first |
| POST | `/{id}/records` | – | 201 `ProspectRecordDto` | Create a new empty record |
| DELETE | `/{id}/records` | – | 204 | Soft-delete all records in a project |

## Project roles

Project roles are stored in `project_members` and are specific to a single project.

| Role | Read project | Edit fields | Edit records | Update/delete project |
|---|---:|---:|---:|---:|
| `OWNER` | yes | yes | yes | yes |
| `ADMIN` | yes | yes | yes | no |
| `MEMBER` | yes | no | yes | no |
| `VIEWER` | yes | no | no | no |

Only memberships with status `ACCEPTED` grant access. A `PENDING` membership is treated as no access.

## Flow: create a project

Creating a project always:

1. Saves the new `Project`.
2. Creates an accepted `ProjectMember` entry with role `OWNER`.
3. Seeds 5 default fields: `contact_name`, `company`, `message_sent`, `replied`, and `reply_content`.

This makes the authenticated user the project owner and ensures that the project can immediately be accessed through the permission system.

```mermaid
sequenceDiagram
    participant C as ProjectsController
    participant S as ProjectServiceImpl
    participant PR as ProjectRepository
    participant PMR as ProjectMemberRepository
    participant FR as ProjectFieldRepository

    C->>S: createProject(user, request)
    S->>PR: save(new Project)
    S->>PMR: save(OWNER, ACCEPTED)
    S->>FR: saveAll(5 default ProjectField)
    S-->>C: ProjectDto
```

## Flow: reorder fields

The request must contain exactly the set of field ids that currently exist on the project — a mismatch (missing id, extra id, or id from another project) is rejected as a client error rather than silently ignored.

```mermaid
sequenceDiagram
    participant C as ProjectsController
    participant S as ProjectServiceImpl
    participant FR as ProjectFieldRepository

    C->>S: reorderFields(user, projectId, fieldIds)
    S->>FR: findAllByProjectOrderByFieldOrderAsc(project)
    S->>S: verify request fieldIds == existing field id set
    Note over S: throws IllegalArgumentException (400) on mismatch
    S->>FR: saveAll(fields with reassigned fieldOrder)
    S-->>C: List<FieldDefinitionDto>
```

## Flow: add a field

```mermaid
sequenceDiagram
    participant C as ProjectsController
    participant S as ProjectServiceImpl
    participant FR as ProjectFieldRepository

    C->>S: addField(user, projectId, request)
    S->>FR: existsByProjectAndKey(project, request.key)
    Note over S: throws FieldKeyConflictException (409) if key already used in this project
    S->>FR: save(new ProjectField)
    S-->>C: FieldDefinitionDto
```

## Ownership checks

Every `{id}` endpoint resolves the project via `findOwnedProject(id, user)`, which queries by `(id, owner)` together — a project belonging to another user returns **404**, not 403, to avoid revealing that the id exists at all.
