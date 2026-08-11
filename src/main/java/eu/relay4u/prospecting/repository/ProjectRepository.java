package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
