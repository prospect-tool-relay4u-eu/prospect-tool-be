# Exceptions and error responses

`exception/GlobalExceptionHandler.java` centralizes error handling, returning RFC-7807 `ProblemDetail` JSON bodies (`type`, `title`, `status`, `detail`, `instance`; validation errors add a non-standard `errors` map of field → message).

## Exception → HTTP status

| Exception | Status | Thrown from |
|---|---|---|
| `ProjectNotFoundException` | 404 Not Found | `findOwnedProject` / `findOwnedRecord` — nearly every `{id}`/`{recordId}` endpoint: project GET/PUT/DELETE, add/remove/reorder fields, list/create/clear/update/delete records |
| `RecordNotFoundException` | 404 Not Found | Requested prospect record does not exist |
| `FieldKeyConflictException` | 409 Conflict | `ProjectServiceImpl.addField` — `POST /api/projects/{id}/fields`, when the field `key` already exists on the project |
| `AccessDeniedException` (Spring Security) | 403 Forbidden | Global — Spring Security itself, for authenticated-but-not-permitted access patterns |
| `MethodArgumentNotValidException` | 400 Bad Request (+ `errors` map) | Any endpoint with a `@Valid` request body: create/update project, create field, reorder fields, update record |
| `IllegalArgumentException` | 400 Bad Request | `reorderFields` when the submitted field-id set doesn't match the project's existing fields; generic fallback elsewhere |

## Access denied cases

`ProjectPermissionServiceImpl` throws `AccessDeniedException` when:

- the user has no membership in the project,
- the membership is not `ACCEPTED`,
- the role does not allow reading,
- the role does not allow editing project fields,
- the role does not allow editing records,
- an owner-only operation is requested by a non-owner.

Examples:

- `VIEWER` tries to create or update a record,
- `MEMBER` tries to add or reorder project fields,
- `ADMIN` tries to delete the project,
- a user with `PENDING` membership tries to read the project,
- a user without membership tries to access project data.

These cases return `403 Forbidden`.

## Not found versus forbidden

The service distinguishes between a missing resource and insufficient permission.

- A project or record that does not exist results in `404 Not Found`.
- A resource that exists but cannot be accessed by the authenticated user results in `403 Forbidden`.

## Authentication errors

`401 Unauthorized` is handled by Spring Security before the request reaches a controller.

**401 Unauthorized** (missing/invalid/expired JWT) never reaches `GlobalExceptionHandler` — it's short-circuited by Spring Security's OAuth2 resource-server filter before the request reaches any controller, so there's no custom `ProblemDetail` body for it. Clients should treat any 401 as "not authenticated, redirect to login" without relying on response body shape.

There is no dedicated `UserNotFoundException` — `UserJwtAuthenticationConverter` always upserts a shadow `User` from valid JWT claims rather than failing, so "no such user" isn't a state this service can observe for an authenticated request.
