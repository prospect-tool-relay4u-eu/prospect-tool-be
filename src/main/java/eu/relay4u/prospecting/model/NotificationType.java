package eu.relay4u.prospecting.model;

public enum NotificationType {
    DEFAULT,
    SYSTEM_INFO,

    // Projects
    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,

    // Fields
    PROJECT_FIELD_ADDED,
    PROJECT_FIELD_DELETED,

    // Records
    RECORDS_IMPORTED,
    RECORDS_CLEARED,
    RECORD_CREATED,
    RECORD_DELETED
}
