package eu.relay4u.prospecting.security;

import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.util.TestDataFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtUtil jwtUtil;
    @Mock UserDetailsService userDetailsService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    @InjectMocks JwtAuthFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Happy path ---

    @Test
    void doFilter_setsAuthentication_forValidToken() throws Exception {
        User user = TestDataFactory.aUser();
        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
        when(jwtUtil.extractAllClaims("valid.token")).thenReturn(claims);
        when(jwtUtil.isTokenExpired(claims)).thenReturn(false);
        when(jwtUtil.extractEmail(claims)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(user);
    }

    @Test
    void doFilter_callsFilterChain_always() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // --- Sad path ---

    @Test
    void doFilter_doesNotSetAuth_whenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_doesNotSetAuth_whenTokenExpired() throws Exception {
        Claims claims = mock(Claims.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token");
        when(jwtUtil.extractAllClaims("expired.token")).thenReturn(claims);
        when(jwtUtil.isTokenExpired(claims)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_doesNotSetAuth_whenJwtExceptionThrown() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
        when(jwtUtil.extractAllClaims("bad.token")).thenThrow(new JwtException("invalid"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_doesNotSetAuth_whenUserLocked() throws Exception {
        User user = TestDataFactory.aUser();
        user.setAccountLocked(true);
        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(jwtUtil.isTokenExpired(claims)).thenReturn(false);
        when(jwtUtil.extractEmail(claims)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_doesNotSetAuth_whenUserDisabled() throws Exception {
        User user = TestDataFactory.aUser();
        user.setIsDeleted(true);
        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(jwtUtil.isTokenExpired(claims)).thenReturn(false);
        when(jwtUtil.extractEmail(claims)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // --- Edge cases ---

    @Test
    void doFilter_doesNotSetAuth_whenHeaderIsBasicAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void doFilter_doesNotSetAuth_whenBearerWithoutToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        Claims claims = mock(Claims.class);
        when(jwtUtil.extractAllClaims("")).thenThrow(new JwtException("empty"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_doesNotOverwriteExistingAuthentication() throws Exception {
        User existingUser = TestDataFactory.aUser();
        existingUser.setEmail("existing@example.com");
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
    }

    @Test
    void getToken_returnsNull_whenHeaderNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void getToken_returnsTokenPart_afterBearerPrefix() throws Exception {
        Claims claims = mock(Claims.class);
        User user = TestDataFactory.aUser();

        when(request.getHeader("Authorization")).thenReturn("Bearer abc123");
        when(jwtUtil.extractAllClaims("abc123")).thenReturn(claims);
        when(jwtUtil.isTokenExpired(claims)).thenReturn(false);
        when(jwtUtil.extractEmail(claims)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        verify(jwtUtil).extractAllClaims("abc123");
    }
}
