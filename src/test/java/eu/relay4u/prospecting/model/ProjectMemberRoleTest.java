package eu.relay4u.prospecting.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProjectMemberRoleTest {

    @Test
    void owner_canReadEditFieldsAndEditRecords() {
        ProjectMemberRole role = ProjectMemberRole.OWNER;
        assertThat(role.isReadAllowed()).isTrue();
        assertThat(role.isFieldEditingAllowed()).isTrue();
        assertThat(role.isRecordEditingAllowed()).isTrue();
    }

    @Test
    void admin_canReadEditFieldsAndEditRecords() {
        ProjectMemberRole role = ProjectMemberRole.ADMIN;
        assertThat(role.isReadAllowed()).isTrue();
        assertThat(role.isFieldEditingAllowed()).isTrue();
        assertThat(role.isRecordEditingAllowed()).isTrue();
    }

    @Test
    void member_canReadAndEditRecordsButCannotEditFields() {
        ProjectMemberRole role = ProjectMemberRole.MEMBER;
        assertThat(role.isReadAllowed()).isTrue();
        assertThat(role.isFieldEditingAllowed()).isFalse();
        assertThat(role.isRecordEditingAllowed()).isTrue();
    }

    @Test
    void viewer_canOnlyRead() {
        ProjectMemberRole role = ProjectMemberRole.VIEWER;
        assertThat(role.isReadAllowed()).isTrue();
        assertThat(role.isFieldEditingAllowed()).isFalse();
        assertThat(role.isRecordEditingAllowed()).isFalse();
    }
}