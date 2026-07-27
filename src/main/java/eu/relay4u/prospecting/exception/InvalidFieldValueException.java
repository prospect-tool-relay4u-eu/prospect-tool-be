package eu.relay4u.prospecting.exception;

import lombok.Getter;

@Getter
public class InvalidFieldValueException extends RuntimeException {
    private final String field;

    public InvalidFieldValueException(String field, String message) {
        super(message);
        this.field = field;
    }
}
