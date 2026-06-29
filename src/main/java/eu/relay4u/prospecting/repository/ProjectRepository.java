package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOwner(User owner);
    Optional<Project> findByIdAndOwner(Long id, User owner);
}
