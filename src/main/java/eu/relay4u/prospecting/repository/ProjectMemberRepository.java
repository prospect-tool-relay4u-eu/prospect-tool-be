package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.ProjectMember;
import eu.relay4u.prospecting.model.ProjectMemberStatus;
import eu.relay4u.prospecting.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserIdAndStatus(
            Long projectId, Long userId, ProjectMemberStatus status
    );

    Page<ProjectMember> findAllByUserAndStatus(
            User user, ProjectMemberStatus status, Pageable pageable
    );

    boolean existsByProjectIdAndInvitedEmail(
            Long projectId, String invitedEmail
    );
}
