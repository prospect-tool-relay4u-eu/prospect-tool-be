package eu.relay4u.prospecting.service.projectpermission;

import eu.relay4u.prospecting.model.User;

public interface ProjectPermissionService {
    void checkReadPermission(Long projectId, User user);

    void checkEditFieldsPermission(Long projectId, User user);

    void checkEditRecordsPermission(Long projectId, User user);

    void checkOwnerPermission(Long projectId, User user);
}
