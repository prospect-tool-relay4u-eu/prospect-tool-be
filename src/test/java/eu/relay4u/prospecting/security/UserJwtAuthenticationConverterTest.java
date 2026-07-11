package eu.relay4u.prospecting.security;

import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.UserRepository;
import eu.relay4u.prospecting.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserJwtAuthenticationConverterTest {

    @Mock UserRepository userRepository;

    @InjectMocks UserJwtAuthenticationConverter converter;

    private Jwt jwtFor(long userId, String email, String name) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", String.valueOf(userId))
                .claim("email", email)
                .claim("name", name)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    // --- Happy path ---

    @Test
    void convert_existingUser_updatesFieldsAndReturnsAuthenticatedToken() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("old@example.com");
        existing.setName("Old Name");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        AbstractAuthenticationToken result = converter.convert(jwtFor(1L, "jane@example.com", "Jane Doe"));

        assertThat(existing.getEmail()).isEqualTo("jane@example.com");
        assertThat(existing.getName()).isEqualTo("Jane Doe");
        assertThat(result.getPrincipal()).isSameAs(existing);
        assertThat(result.isAuthenticated()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void convert_newUser_createsUserFromClaims() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        AbstractAuthenticationToken result = converter.convert(jwtFor(42L, "new@example.com", "New User"));

        ArgumentCaptor<User> captor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getName()).isEqualTo("New User");
        assertThat(result.getPrincipal()).isSameAs(saved);
    }

    // --- Edge cases ---

    @Test
    void convert_setsJwtAsCredentials() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestDataFactory.aUser()));
        Jwt jwt = jwtFor(1L, "jane@example.com", "Jane Doe");

        AbstractAuthenticationToken result = converter.convert(jwt);

        assertThat(result.getCredentials()).isSameAs(jwt);
    }

    @Test
    void convert_grantsNoAuthorities() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestDataFactory.aUser()));

        AbstractAuthenticationToken result = converter.convert(jwtFor(1L, "jane@example.com", "Jane Doe"));

        assertThat(result.getAuthorities()).isEmpty();
    }
}
