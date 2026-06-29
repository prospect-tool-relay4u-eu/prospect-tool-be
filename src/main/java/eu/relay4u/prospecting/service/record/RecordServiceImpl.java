package eu.relay4u.prospecting.service.record;

import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.dto.record.UpdateRecordRequest;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.model.Project;
import eu.relay4u.prospecting.model.ProspectRecord;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.ProjectRepository;
import eu.relay4u.prospecting.repository.ProspectRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final ProjectRepository projectRepository;
    private final ProspectRecordRepository prospectRecordRepository;

    @Override
    public List<ProspectRecordDto> getRecords(Long projectId, User user) {
        Project project = findOwnedProject(projectId, user);
        return prospectRecordRepository.findAllByProjectOrderByCreatedAtAsc(project)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ProspectRecordDto createRecord(Long projectId, User user) {
        Project project = findOwnedProject(projectId, user);
        ProspectRecord record = new ProspectRecord();
        record.setProject(project);
        prospectRecordRepository.save(record);
        return toDto(record);
    }

    @Override
    @Transactional
    public ProspectRecordDto updateRecord(UUID recordId, UpdateRecordRequest request, User user) {
        ProspectRecord record = findOwnedRecord(recordId, user);
        record.getValues().putAll(request.values());
        prospectRecordRepository.save(record);
        return toDto(record);
    }

    @Override
    @Transactional
    public void deleteRecord(UUID recordId, User user) {
        ProspectRecord record = findOwnedRecord(recordId, user);
        prospectRecordRepository.delete(record);
    }

    @Override
    @Transactional
    public void clearAllRecords(Long projectId, User user) {
        Project project = findOwnedProject(projectId, user);
        prospectRecordRepository.softDeleteAllByProject(project);
    }

    private Project findOwnedProject(Long id, User user) {
        return projectRepository.findByIdAndOwner(id, user)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private ProspectRecord findOwnedRecord(UUID recordId, User user) {
        return prospectRecordRepository.findById(recordId)
                .filter(r -> r.getProject().getOwner().getId().equals(user.getId()))
                .orElseThrow(ProjectNotFoundException::new);
    }

    private ProspectRecordDto toDto(ProspectRecord record) {
        return new ProspectRecordDto(
                record.getId(),
                record.getProject().getId(),
                record.getValues(),
                record.getCreatedAt()
        );
    }
}
