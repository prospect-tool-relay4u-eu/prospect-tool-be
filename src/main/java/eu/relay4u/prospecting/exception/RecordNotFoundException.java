package eu.relay4u.prospecting.exception;

public class RecordNotFoundException extends RuntimeException {
    public RecordNotFoundException() {
        super("Record not found");
    }
}
