package eu.relay4u.prospecting.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IssuerJwtValidatorTest {

    private final IssuerJwtValidator validator = new IssuerJwtValidator("https://auth.relay4u.eu");

    private Jwt jwtWithIssuer(String issuer) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "1")
                .claim("iss", issuer)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void validate_succeeds_whenIssuerMatches() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer("https://auth.relay4u.eu"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_fails_whenIssuerDoesNotMatch() {
        OAuth2TokenValidatorResult result = validator.validate(jwtWithIssuer("https://malicious.example.com"));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void validate_fails_whenIssuerClaimMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }
}
