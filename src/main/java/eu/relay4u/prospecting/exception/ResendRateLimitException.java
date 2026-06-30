package eu.relay4u.prospecting.exception;

public class ResendRateLimitException extends RuntimeException {
    public ResendRateLimitException(String message) {
        super(message);
    }
}
