package eu.relay4u.prospecting.exception;

import eu.relay4u.prospecting.filter.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ProblemDetail buildProblem(HttpStatus status, ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code.name());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail handleProjectNotFound(ProjectNotFoundException ex) {
        log.info("Project not found: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RecordNotFoundException.class)
    public ProblemDetail handleRecordNotFound(RecordNotFoundException ex) {
        log.info("Record not found: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, ErrorCode.RECORD_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(FieldKeyConflictException.class)
    public ProblemDetail handleFieldKeyConflict(FieldKeyConflictException ex) {
        log.info("Field key conflict: {}", ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, ErrorCode.FIELD_KEY_CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidFieldValueException.class)
    public ProblemDetail handleInvalidFieldValue(InvalidFieldValueException ex) {
        log.info("Invalid field value for '{}': {}", ex.getField(), ex.getMessage());
        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_FIELD_VALUE, ex.getMessage());
        problem.setProperty("errors", Map.of(ex.getField(), ex.getMessage()));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (existing, duplicate) -> existing + "; " + duplicate
                ));
        log.info("Validation failed: {}", errors);
        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Please try again later.");
    }
}
