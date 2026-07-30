package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
        boolean existsByInvitedEmail(String invitedEmail);
}
