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
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${security.password.pepper}")
    private String pepper;

    @Value("${security.lockout.max-attempts}")
    private int maxAttempts;

    @Value("${security.lockout.duration-minutes}")
    private int lockoutDuration;


    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        validRegisterData(request);

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password() + pepper));
        return userMapper.toDto(userRepository.save(user));
    }

    private void validRegisterData(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())
                || userRepository.findUserByEmail(request.email()).isPresent()) {
            throw new RegisterException("Invalid register data");
        }
    }

    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findUserByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        isUserLocked(user);

        try {
            return getAuthenticationResponse(request, user);
        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw e;
        }
    }

    private void isUserLocked(User user) {
        if (user.getAccountLocked()) {
            if (user.getLockTime().isBefore(LocalDateTime.now())) {
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            } else {
                throw new LockedException("Account is locked. Try again later.");
            }
        }
    }

    private @NonNull AuthenticationResponse getAuthenticationResponse(LoginRequest request, User user) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password() + pepper
                )
        );

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        return new AuthenticationResponse(
                jwtUtil.generateToken(authenticate.getName())
        );
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now().plusMinutes(lockoutDuration));
        }
        userRepository.save(user);
    }
}
