package eu.relay4u.prospecting.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // --- Happy path ---

    @Test
    void handleProjectNotFound_returns404WithMessage() {
        ProblemDetail result = handler.handleProjectNotFound(new ProjectNotFoundException());

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo("Project not found");
        assertThat(result.getProperties()).containsEntry("code", ErrorCode.PROJECT_NOT_FOUND.name());
    }

    @Test
    void handleFieldKeyConflict_returns409WithMessage() {
        ProblemDetail result = handler.handleFieldKeyConflict(new FieldKeyConflictException("my_key"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getDetail()).contains("my_key");
    }

    @Test
    void handleInvalidFieldValue_returns400WithFieldError() {
        ProblemDetail result = handler.handleInvalidFieldValue(
                new InvalidFieldValueException("age", "Value for field 'age' must be of type INTEGER."));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getProperties()).containsEntry("code", ErrorCode.INVALID_FIELD_VALUE.name());
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) result.getProperties().get("errors");
        assertThat(errors).containsKey("age");
    }

    @Test
    void handleAccessDenied_returns403() {
        ProblemDetail result = handler.handleAccessDenied(new AccessDeniedException("Forbidden"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void handleUnexpected_returns500WithGenericDetailAndInternalErrorCode() {
        ProblemDetail result = handler.handleUnexpected(new RuntimeException("db connection refused"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getProperties()).containsEntry("code", ErrorCode.INTERNAL_ERROR.name());
        assertThat(result.getDetail()).doesNotContain("db connection refused");
    }

    @Test
    void handleValidation_returns400WithErrorsMap() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error = new FieldError("obj", "name", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));

        ProblemDetail result = handler.handleValidation(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) result.getProperties().get("errors");
        assertThat(errors).containsEntry("name", "must not be blank");
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        ProblemDetail result = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("bad input");
    }

    // --- Edge cases ---

    @Test
    void handleValidation_withMultipleFieldErrors_includesAll() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "name", "must not be blank"),
                new FieldError("obj", "email", "must be a valid email")
        ));

        ProblemDetail result = handler.handleValidation(ex);

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) result.getProperties().get("errors");
        assertThat(errors).hasSize(2)
                .containsEntry("name", "must not be blank")
                .containsEntry("email", "must be a valid email");
    }

    @Test
    void handleValidation_withNullDefaultMessage_usesPlaceholder() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError errorWithNullMessage = new FieldError("obj", "field", null, false, null, null, null);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(errorWithNullMessage));

        ProblemDetail result = handler.handleValidation(ex);

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) result.getProperties().get("errors");
        assertThat(errors.get("field")).isEqualTo("invalid");
    }
}
