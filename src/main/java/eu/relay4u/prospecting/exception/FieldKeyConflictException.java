package eu.relay4u.prospecting.exception;

public class FieldKeyConflictException extends RuntimeException {
    public FieldKeyConflictException(String key) {
        super("Field key already exists in this project: " + key);
    }
}
