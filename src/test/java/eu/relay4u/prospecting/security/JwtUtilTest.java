package eu.relay4u.prospecting.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("polkmiujhbtrfdxsqsdxcwedfgvbthjm");
        ReflectionTestUtils.setField(jwtUtil, "expirationInHours", 1L);
    }

    // --- Happy path ---

    @Test
    void generateToken_returnsNonNullString() {
        assertThat(jwtUtil.generateToken("test@example.com")).isNotNull().isNotBlank();
    }

    @Test
    void extractAllClaims_returnsSubjectMatchingEmail() {
        String token = jwtUtil.generateToken("test@example.com");

        Claims claims = jwtUtil.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("test@example.com");
    }

    @Test
    void isTokenExpired_returnsFalse_forFreshToken() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 3_600_000));

        assertThat(jwtUtil.isTokenExpired(claims)).isFalse();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@example.com");

        assertThat(jwtUtil.extractEmail(claims)).isEqualTo("user@example.com");
    }

    // --- Sad path ---

    @Test
    void extractAllClaims_throwsJwtException_forInvalidToken() {
        assertThatThrownBy(() -> jwtUtil.extractAllClaims("not.a.valid.token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractAllClaims_throwsJwtException_forTamperedToken() {
        String token = jwtUtil.generateToken("test@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtUtil.extractAllClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void isTokenExpired_returnsTrue_forExpiredClaims() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 60_000));

        assertThat(jwtUtil.isTokenExpired(claims)).isTrue();
    }

    // --- Edge cases ---

    @Test
    void generateToken_expirationIsApproximatelyOneHourFromNow() {
        long before = System.currentTimeMillis();
        String token = jwtUtil.generateToken("test@example.com");
        long after = System.currentTimeMillis();

        Claims claims = jwtUtil.extractAllClaims(token);
        long expMs = claims.getExpiration().getTime();

        assertThat(expMs).isBetween(before + 3_595_000L, after + 3_605_000L);
    }

    @Test
    void generateToken_withEmailContainingSpecialChars_producesValidToken() {
        String email = "user+tag.test@sub.example.com";
        String token = jwtUtil.generateToken(email);

        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(email);
    }

    @Test
    void extractAllClaims_throwsJwtException_forExpiredToken() {
        JwtUtil expiredUtil = new JwtUtil("polkmiujhbtrfdxsqsdxcwedfgvbthjm");
        ReflectionTestUtils.setField(expiredUtil, "expirationInHours", -1L);
        String expiredToken = expiredUtil.generateToken("test@example.com");

        assertThatThrownBy(() -> jwtUtil.extractAllClaims(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void generateToken_forDifferentEmails_produceDifferentTokens() {
        String token1 = jwtUtil.generateToken("user1@example.com");
        String token2 = jwtUtil.generateToken("user2@example.com");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateToken_subjectMatchesInputEmail() {
        String email = "specific@domain.com";
        String token = jwtUtil.generateToken(email);

        assertThat(jwtUtil.extractEmail(jwtUtil.extractAllClaims(token))).isEqualTo(email);
    }
}
