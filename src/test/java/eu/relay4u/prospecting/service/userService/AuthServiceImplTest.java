package eu.relay4u.prospecting.service.userService;

import eu.relay4u.prospecting.dto.UserDto;
import eu.relay4u.prospecting.dto.login.AuthenticationResponse;
import eu.relay4u.prospecting.dto.login.LoginRequest;
import eu.relay4u.prospecting.dto.register.RegisterRequest;
import eu.relay4u.prospecting.dto.verification.ResendVerificationRequest;
import eu.relay4u.prospecting.dto.verification.VerifyEmailRequest;
import eu.relay4u.prospecting.exception.*;
import eu.relay4u.prospecting.mapper.UserMapper;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.UserRepository;
import eu.relay4u.prospecting.security.JwtUtil;
import eu.relay4u.prospecting.service.email.EmailService;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtil jwtUtil;
    @Mock EmailService emailService;

    @InjectMocks AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "pepper", "test-pepper");
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutDuration", 10);
        ReflectionTestUtils.setField(authService, "verificationCodeExpiryMinutes", 15);
        ReflectionTestUtils.setField(authService, "maxVerificationAttempts", 5);
        ReflectionTestUtils.setField(authService, "maxResendPerHour", 3);
    }

    private static String sha256(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- Happy path ---

    @Test
    void register_encodesPasswordWithPepper() {
        RegisterRequest request = TestDataFactory.validRegisterRequest();
        User user = TestDataFactory.aUser();
        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(new UserDto(1L, "Test User", "test@example.com"));

        authService.register(request);

        verify(passwordEncoder).encode("Password1!test-pepper");
    }

    @Test
    void register_savesUserAndReturnsDto() {
        RegisterRequest request = TestDataFactory.validRegisterRequest();
        User user = TestDataFactory.aUser();
        UserDto expectedDto = new UserDto(1L, "Test User", "test@example.com");

        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        UserDto result = authService.register(request);

        verify(userRepository).save(user);
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void login_returnsTokenOnSuccess() {
        User user = TestDataFactory.aUser();
        LoginRequest request = new LoginRequest("test@example.com", "Password1!");
        Authentication auth = mock(Authentication.class);

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getName()).thenReturn("test@example.com");
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token");

        AuthenticationResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_resetsFailedAttempts_onSuccess() {
        User user = TestDataFactory.aUser();
        user.setFailedLoginAttempts(3);
        LoginRequest request = new LoginRequest("test@example.com", "Password1!");
        Authentication auth = mock(Authentication.class);

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getName()).thenReturn("test@example.com");
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.login(request);

        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void login_unlocksAccount_whenLockExpired() {
        User user = TestDataFactory.aUser();
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now().minusMinutes(15));
        LoginRequest request = new LoginRequest("test@example.com", "Password1!");
        Authentication auth = mock(Authentication.class);

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getName()).thenReturn("test@example.com");
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.login(request);

        assertThat(user.getAccountLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    // --- Sad path ---

    @Test
    void register_throwsRegisterException_whenPasswordsDontMatch() {
        RegisterRequest request = new RegisterRequest("User", "user@example.com", "Password1!", "Different1!");
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RegisterException.class);
    }

    @Test
    void register_throwsRegisterException_whenEmailExists() {
        RegisterRequest request = TestDataFactory.validRegisterRequest();
        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.of(TestDataFactory.aUser()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RegisterException.class);
    }

    @Test
    void login_throwsBadCredentialsException_whenUserNotFound() {
        when(userRepository.findUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "Password1!")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsLockedException_whenAccountStillLocked() {
        User user = TestDataFactory.aUser();
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "Password1!")))
                .isInstanceOf(LockedException.class);
    }

    @Test
    void login_incrementsFailedAttempts_onBadCredentials() {
        User user = TestDataFactory.aUser();
        user.setFailedLoginAttempts(0);
        LoginRequest request = new LoginRequest("test@example.com", "WrongPass1!");

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_locksAccount_whenMaxAttemptsReached() {
        User user = TestDataFactory.aUser();
        user.setFailedLoginAttempts(4);

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "Wrong1!")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getAccountLocked()).isTrue();
        assertThat(user.getLockTime()).isAfter(LocalDateTime.now());
    }

    // --- Edge cases ---

    @Test
    void login_locksAccount_atExactlyMaxAttempts() {
        User user = TestDataFactory.aUser();
        user.setFailedLoginAttempts(4); // next attempt (5th) = maxAttempts

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "Wrong1!")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getAccountLocked()).isTrue();
    }

    @Test
    void login_doesNotLock_belowMaxAttempts() {
        User user = TestDataFactory.aUser();
        user.setFailedLoginAttempts(3); // next attempt = 4, below max of 5

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "Wrong1!")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getAccountLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
    }

    @Test
    void login_unlocksAtExactLockDurationBoundary() {
        User user = TestDataFactory.aUser();
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now().minusMinutes(10).minusSeconds(1)); // just past 10 min
        Authentication auth = mock(Authentication.class);

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getName()).thenReturn("test@example.com");
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.login(new LoginRequest("test@example.com", "Password1!"));

        assertThat(user.getAccountLocked()).isFalse();
    }

    @Test
    void login_pepperAppendedToPasswordBeforeAuthentication() {
        User user = TestDataFactory.aUser();
        LoginRequest request = new LoginRequest("test@example.com", "Password1!");
        Authentication auth = mock(Authentication.class);

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getName()).thenReturn("test@example.com");
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.login(request);

        verify(authenticationManager).authenticate(
                argThat(token -> token instanceof UsernamePasswordAuthenticationToken a
                        && "Password1!test-pepper".equals(a.getCredentials()))
        );
    }

    @Test
    void login_setsFutureLockTime_whenLocking() {
        User user = TestDataFactory.aUser();
        user.setFailedLoginAttempts(4);

        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "Wrong1!")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getLockTime()).isAfter(LocalDateTime.now().plusMinutes(9));
    }

    // --- register — email verification ---

    @Test
    void register_setsEmailVerifiedFalse_andCallsEmailService() {
        RegisterRequest request = TestDataFactory.validRegisterRequest();
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(new UserDto(1L, "Test User", "test@example.com"));

        authService.register(request);

        assertThat(user.getEmailVerified()).isFalse();
        verify(emailService).sendVerificationCode(eq(user.getEmail()), eq(user.getName()), any());
    }

    @Test
    void register_propagatesException_whenEmailServiceThrows() {
        RegisterRequest request = TestDataFactory.validRegisterRequest();
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        doThrow(new RuntimeException("Email failed")).when(emailService)
                .sendVerificationCode(any(), any(), any());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email failed");
    }

    // --- login — email not verified ---

    @Test
    void login_throwsEmailNotVerifiedException_whenEmailNotVerified() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "Password1!")))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    // --- verifyEmail ---

    @Test
    void verifyEmail_success_setsEmailVerifiedAndClearsCode() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setVerificationCode(sha256("123456"));
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.verifyEmail(new VerifyEmailRequest("test@example.com", "123456"));

        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getVerificationCode()).isNull();
        assertThat(user.getVerificationCodeExpiry()).isNull();
        assertThat(user.getVerificationAttempts()).isZero();
    }

    @Test
    void verifyEmail_throwsInvalidCode_whenUserNotFound() {
        when(userRepository.findUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("unknown@example.com", "123456")))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    void verifyEmail_throwsAlreadyVerified_whenAlreadyVerified() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(true);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("test@example.com", "123456")))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
    }

    @Test
    void verifyEmail_throwsVerificationBlocked_whenAttemptsExhausted() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setVerificationAttempts(5);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("test@example.com", "123456")))
                .isInstanceOf(VerificationBlockedException.class);
    }

    @Test
    void verifyEmail_throwsExpired_whenCodeExpired() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setVerificationCode(sha256("123456"));
        user.setVerificationCodeExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("test@example.com", "123456")))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verifyEmail_throwsExpired_whenCodeExpiryIsNull() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setVerificationCode(sha256("123456"));
        user.setVerificationCodeExpiry(null);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("test@example.com", "123456")))
                .isInstanceOf(VerificationCodeExpiredException.class);
    }

    @Test
    void verifyEmail_throwsInvalidCode_andIncrementsAttempts_whenWrongCode() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setVerificationCode(sha256("999999"));
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        user.setVerificationAttempts(0);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("test@example.com", "123456")))
                .isInstanceOf(InvalidVerificationCodeException.class);

        assertThat(user.getVerificationAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    // --- resendVerification ---

    @Test
    void resendVerification_success_generatesNewCodeAndSendsEmail() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setResendCount(0);
        user.setLastResendAt(null);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification(new ResendVerificationRequest("test@example.com"));

        assertThat(user.getVerificationCode()).isNotNull();
        assertThat(user.getVerificationCodeExpiry()).isAfter(LocalDateTime.now());
        assertThat(user.getResendCount()).isEqualTo(1);
        assertThat(user.getVerificationAttempts()).isZero();
        verify(emailService).sendVerificationCode(eq(user.getEmail()), eq(user.getName()), any());
    }

    @Test
    void resendVerification_throwsRegisterException_whenUserNotFound() {
        when(userRepository.findUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resendVerification(new ResendVerificationRequest("unknown@example.com")))
                .isInstanceOf(RegisterException.class);
    }

    @Test
    void resendVerification_throwsAlreadyVerified_whenAlreadyVerified() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(true);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resendVerification(new ResendVerificationRequest("test@example.com")))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
    }

    @Test
    void resendVerification_throwsRateLimit_whenMaxResendReached() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setResendCount(3);
        user.setLastResendAt(LocalDateTime.now().minusMinutes(10));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resendVerification(new ResendVerificationRequest("test@example.com")))
                .isInstanceOf(ResendRateLimitException.class);
    }

    @Test
    void resendVerification_resetsWindowAndAllows_whenHourElapsed() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setResendCount(3);
        user.setLastResendAt(LocalDateTime.now().minusHours(2));
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification(new ResendVerificationRequest("test@example.com"));

        assertThat(user.getResendCount()).isEqualTo(1);
        verify(emailService).sendVerificationCode(any(), any(), any());
    }

    @Test
    void resendVerification_resetsVerificationAttempts_onResend() {
        User user = TestDataFactory.aUser();
        user.setEmailVerified(false);
        user.setResendCount(1);
        user.setLastResendAt(LocalDateTime.now().minusMinutes(5));
        user.setVerificationAttempts(4);
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification(new ResendVerificationRequest("test@example.com"));

        assertThat(user.getVerificationAttempts()).isZero();
    }
}
