package eu.relay4u.prospecting.security;

import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.UserRepository;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks CustomUserDetailsService service;

    @Test
    void loadUserByUsername_returnsUser_whenFound() {
        User user = TestDataFactory.aUser();
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("test@example.com");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenNotFound() {
        when(userRepository.findUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_returnsUserImplementingUserDetails() {
        User user = TestDataFactory.aUser();
        when(userRepository.findUserByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("test@example.com");

        assertThat(result).isInstanceOf(UserDetails.class);
        assertThat(result.getUsername()).isEqualTo("test@example.com");
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isEnabled()).isTrue();
    }
}
