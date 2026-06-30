package eu.relay4u.prospecting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.relay4u.prospecting.dto.UserDto;
import eu.relay4u.prospecting.dto.login.AuthenticationResponse;
import eu.relay4u.prospecting.dto.login.LoginRequest;
import eu.relay4u.prospecting.dto.register.RegisterRequest;
import eu.relay4u.prospecting.dto.verification.ResendVerificationRequest;
import eu.relay4u.prospecting.dto.verification.VerifyEmailRequest;
import eu.relay4u.prospecting.exception.*;
import eu.relay4u.prospecting.service.userService.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;
    @InjectMocks AuthController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // --- POST /api/auth/register ---

    @Test
    void register_returns201() throws Exception {
        when(authService.register(any())).thenReturn(new UserDto(1L, "Test User", "test@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Test User", "test@example.com", "Password1!", "Password1!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_returns400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("User", "not-an-email", "Password1!", "Password1!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400_whenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("User", "test@example.com", "short", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("", "test@example.com", "Password1!", "Password1!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400_whenPasswordsDontMatch() throws Exception {
        when(authService.register(any())).thenThrow(new RegisterException("Invalid register data"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("User", "test@example.com", "Password1!", "Different1!"))))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/auth/login ---

    @Test
    void login_returns200_withToken() throws Exception {
        when(authService.login(any())).thenReturn(new AuthenticationResponse("jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_returns401_whenBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "WrongPass1!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns423_whenAccountLocked() throws Exception {
        when(authService.login(any())).thenThrow(new LockedException("Account is locked. Try again later."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "Password1!"))))
                .andExpect(status().isLocked());
    }

    @Test
    void login_returns400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("not-an-email", "Password1!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns403_whenEmailNotVerified() throws Exception {
        when(authService.login(any())).thenThrow(new EmailNotVerifiedException("Account not verified."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "Password1!"))))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/auth/verify-email ---

    @Test
    void verifyEmail_returns200_onSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyEmailRequest("test@example.com", "123456"))))
                .andExpect(status().isOk());
    }

    @Test
    void verifyEmail_returns400_whenCodeFormatInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyEmailRequest("test@example.com", "abcdef"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_returns400_whenInvalidCode() throws Exception {
        doThrow(new InvalidVerificationCodeException("Invalid code."))
                .when(authService).verifyEmail(any());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyEmailRequest("test@example.com", "123456"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_returns400_whenCodeExpired() throws Exception {
        doThrow(new VerificationCodeExpiredException("Code expired."))
                .when(authService).verifyEmail(any());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyEmailRequest("test@example.com", "123456"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_returns423_whenBlocked() throws Exception {
        doThrow(new VerificationBlockedException("Blocked."))
                .when(authService).verifyEmail(any());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyEmailRequest("test@example.com", "123456"))))
                .andExpect(status().isLocked());
    }

    // --- POST /api/auth/resend-verification ---

    @Test
    void resendVerification_returns200_onSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResendVerificationRequest("test@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void resendVerification_returns429_whenRateLimited() throws Exception {
        doThrow(new ResendRateLimitException("Rate limit exceeded."))
                .when(authService).resendVerification(any());

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResendVerificationRequest("test@example.com"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void resendVerification_returns400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResendVerificationRequest("not-an-email"))))
                .andExpect(status().isBadRequest());
    }
}
