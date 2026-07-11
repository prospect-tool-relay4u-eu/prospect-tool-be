# Exceptions and error responses

`exception/GlobalExceptionHandler.java` centralizes error handling, returning RFC-7807 `ProblemDetail` JSON bodies (`type`, `title`, `status`, `detail`, `instance`; validation errors add a non-standard `errors` map of field → message).

## Exception → HTTP status

| Exception | Status | Thrown from |
|---|---|---|
| `ProjectNotFoundException` | 404 Not Found | `findOwnedProject` / `findOwnedRecord` — nearly every `{id}`/`{recordId}` endpoint: project GET/PUT/DELETE, add/remove/reorder fields, list/create/clear/update/delete records |
| `FieldKeyConflictException` | 409 Conflict | `ProjectServiceImpl.addField` — `POST /api/projects/{id}/fields`, when the field `key` already exists on the project |
| `AccessDeniedException` (Spring Security) | 403 Forbidden | Global — Spring Security itself, for authenticated-but-not-permitted access patterns |
| `MethodArgumentNotValidException` | 400 Bad Request (+ `errors` map) | Any endpoint with a `@Valid` request body: create/update project, create field, reorder fields, update record |
| `IllegalArgumentException` | 400 Bad Request | `reorderFields` when the submitted field-id set doesn't match the project's existing fields; generic fallback elsewhere |

## Not handled here

**401 Unauthorized** (missing/invalid/expired JWT) never reaches `GlobalExceptionHandler` — it's short-circuited by Spring Security's OAuth2 resource-server filter before the request reaches any controller, so there's no custom `ProblemDetail` body for it. Clients should treat any 401 as "not authenticated, redirect to login" without relying on response body shape.

There is no dedicated `UserNotFoundException` — `UserJwtAuthenticationConverter` always upserts a shadow `User` from valid JWT claims rather than failing, so "no such user" isn't a state this service can observe for an authenticated request.
