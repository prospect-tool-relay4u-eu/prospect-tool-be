package eu.relay4u.prospecting.util;

import eu.relay4u.prospecting.dto.field.CreateFieldRequest;
import eu.relay4u.prospecting.dto.project.CreateProjectRequest;
import eu.relay4u.prospecting.dto.register.RegisterRequest;
import eu.relay4u.prospecting.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

public class TestDataFactory {

    public static User aUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("$encoded$password$");
        user.setIsDeleted(false);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setEmailVerified(true);
        user.setVerificationAttempts(0);
        user.setResendCount(0);
        return user;
    }

    public static Project aProject(User owner) {
        Project project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("Test Description");
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());
        project.setIsDeleted(false);
        return project;
    }

    public static ProjectField aField(Project project) {
        ProjectField field = new ProjectField();
        field.setId(UUID.randomUUID());
        field.setProject(project);
        field.setKey("contact_name");
        field.setLabel("Full Name");
        field.setType(FieldType.STRING);
        field.setRequired(false);
        field.setFieldOrder(0);
        field.setCreatedAt(LocalDateTime.now());
        field.setIsDeleted(false);
        return field;
    }

    public static ProspectRecord aRecord(Project project) {
        ProspectRecord record = new ProspectRecord();
        record.setId(UUID.randomUUID());
        record.setProject(project);
        record.setValues(new HashMap<>());
        record.setCreatedAt(LocalDateTime.now());
        record.setIsDeleted(false);
        return record;
    }

    public static CreateProjectRequest createProjectRequest() {
        return new CreateProjectRequest("New Project", "Description");
    }

    public static RegisterRequest validRegisterRequest() {
        return new RegisterRequest("Test User", "test@example.com", "Password1!", "Password1!");
    }

    public static CreateFieldRequest createFieldRequest() {
        return new CreateFieldRequest("new_key", "New Label", FieldType.STRING, false, 5);
    }
}
