package eu.relay4u.prospecting.service.userService;

import eu.relay4u.prospecting.dto.UserDto;
import eu.relay4u.prospecting.dto.login.AuthenticationResponse;
import eu.relay4u.prospecting.dto.login.LoginRequest;
import eu.relay4u.prospecting.dto.register.RegisterRequest;
import eu.relay4u.prospecting.exception.RegisterException;
import eu.relay4u.prospecting.mapper.UserMapper;
import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.UserRepository;
import eu.relay4u.prospecting.security.JwtUtil;
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

import java.time.LocalDateTime;
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

    @InjectMocks AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "pepper", "test-pepper");
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutDuration", 10);
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
}
