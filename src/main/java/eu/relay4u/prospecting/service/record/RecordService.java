package eu.relay4u.prospecting.service.record;

import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.dto.record.UpdateRecordRequest;
import eu.relay4u.prospecting.model.User;

import java.util.List;
import java.util.UUID;

public interface RecordService {
    List<ProspectRecordDto> getRecords(Long projectId, User user);
    ProspectRecordDto createRecord(Long projectId, User user);
    ProspectRecordDto updateRecord(UUID recordId, UpdateRecordRequest request, User user);
    void deleteRecord(UUID recordId, User user);
    void clearAllRecords(Long projectId, User user);
}
