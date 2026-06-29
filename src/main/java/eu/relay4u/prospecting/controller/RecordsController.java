package eu.relay4u.prospecting.controller;

import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.dto.record.UpdateRecordRequest;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.service.record.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordsController {

    private final RecordService recordService;

    @PutMapping("/{recordId}")
    public ProspectRecordDto updateDataInRecord(@PathVariable UUID recordId,
                                                @RequestBody @Valid UpdateRecordRequest request,
                                                @AuthenticationPrincipal User user) {
        return recordService.updateRecord(recordId, request, user);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRecord(@PathVariable UUID recordId, @AuthenticationPrincipal User user) {
        recordService.deleteRecord(recordId, user);
        return ResponseEntity.noContent().build();
    }
}
