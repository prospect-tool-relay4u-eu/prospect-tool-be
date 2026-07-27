package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findAllByOwner(User owner, Pageable pageable);
    Optional<Project> findByIdAndOwner(Long id, User owner);
}
