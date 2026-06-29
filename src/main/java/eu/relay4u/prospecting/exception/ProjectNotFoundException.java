package eu.relay4u.prospecting.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException() {
        super("Project not found");
    }
}
