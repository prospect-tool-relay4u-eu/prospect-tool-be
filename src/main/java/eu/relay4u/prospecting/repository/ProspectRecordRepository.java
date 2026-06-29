package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProspectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProspectRecordRepository extends JpaRepository<ProspectRecord, UUID> {
    List<ProspectRecord> findAllByProjectOrderByCreatedAtAsc(Project project);
    long countByProject(Project project);

    @Modifying
    @Query("UPDATE ProspectRecord r SET r.isDeleted = true WHERE r.project = :project AND r.isDeleted = false")
    void softDeleteAllByProject(@Param("project") Project project);
}
