package eu.relay4u.prospecting.service.record;

import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.dto.record.UpdateRecordRequest;
import eu.relay4u.prospecting.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecordService {
    Page<ProspectRecordDto> getRecords(Long projectId, User user, Pageable pageable);
    ProspectRecordDto createRecord(Long projectId, User user);
    ProspectRecordDto updateRecord(UUID recordId, UpdateRecordRequest request, User user);
    void deleteRecord(UUID recordId, User user);
    void clearAllRecords(Long projectId, User user);
}
