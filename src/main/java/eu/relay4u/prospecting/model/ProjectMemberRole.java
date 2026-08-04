package eu.relay4u.prospecting.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectMemberRole {
    OWNER(true, true, true),
    ADMIN(true, true, true),
    MEMBER(true, false, true),
    VIEWER(true, false, false);

    private final boolean readAllowed;
    private final boolean fieldEditingAllowed;
    private final boolean recordEditingAllowed;
}
