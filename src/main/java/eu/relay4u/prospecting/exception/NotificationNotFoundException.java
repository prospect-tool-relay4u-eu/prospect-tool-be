package eu.relay4u.prospecting.exception;

public class NotificationNotFoundException extends RuntimeException{
    public NotificationNotFoundException() {
        super("notification not found");
    }
}
