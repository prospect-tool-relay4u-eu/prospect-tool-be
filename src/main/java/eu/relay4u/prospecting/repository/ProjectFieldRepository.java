package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProjectField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectFieldRepository extends JpaRepository<ProjectField, UUID> {
    List<ProjectField> findAllByProjectOrderByFieldOrderAsc(Project project);
    boolean existsByProjectAndKey(Project project, String key);
    long countByProject(Project project);
}
