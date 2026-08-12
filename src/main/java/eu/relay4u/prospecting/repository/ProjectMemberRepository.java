package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    boolean existsByProjectIdAndInvitedEmail(Long projectID, String invitedEmail);

    Optional<ProjectMember> findByProjectIdAndInvitedEmail(Long projectId, String invitedEmail);
}
