package eu.relay4u.prospecting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.relay4u.prospecting.dto.record.ProspectRecordDto;
import eu.relay4u.prospecting.dto.record.UpdateRecordRequest;
import eu.relay4u.prospecting.exception.GlobalExceptionHandler;
import eu.relay4u.prospecting.exception.ProjectNotFoundException;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.service.record.RecordService;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecordsControllerTest {

    @Mock RecordService recordService;
    @InjectMocks RecordsController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    User mockUser;
    UUID recordId;
    ProspectRecordDto recordDto;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();

        mockUser = TestDataFactory.aUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities())
        );

        recordId = UUID.randomUUID();
        recordDto = new ProspectRecordDto(recordId, 1L, Map.of("name", "Anna"), LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateRecord_returns200() throws Exception {
        when(recordService.updateRecord(eq(recordId), any(), any())).thenReturn(recordDto);

        mockMvc.perform(put("/api/records/" + recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRecordRequest(Map.of("name", "Anna")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.name").value("Anna"));
    }

    @Test
    void updateRecord_returns404_whenNotFound() throws Exception {
        when(recordService.updateRecord(eq(recordId), any(), any())).thenThrow(new ProjectNotFoundException());

        mockMvc.perform(put("/api/records/" + recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRecordRequest(Map.of()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecord_returns400_whenValuesNull() throws Exception {
        mockMvc.perform(put("/api/records/" + recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRecord_returns204() throws Exception {
        mockMvc.perform(delete("/api/records/" + recordId))
                .andExpect(status().isNoContent());

        verify(recordService).deleteRecord(eq(recordId), any());
    }

    @Test
    void deleteRecord_returns404_whenNotFound() throws Exception {
        doThrow(new ProjectNotFoundException()).when(recordService).deleteRecord(eq(recordId), any());

        mockMvc.perform(delete("/api/records/" + recordId))
                .andExpect(status().isNotFound());
    }
}
