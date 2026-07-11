package eu.relay4u.prospecting.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class IssuerJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_ISSUER =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The token issuer is invalid", null);

    private final String expectedIssuer;

    public IssuerJwtValidator(String expectedIssuer) {
        this.expectedIssuer = expectedIssuer;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String actualIssuer = token.getClaimAsString("iss");
        if (expectedIssuer.equals(actualIssuer)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
    }
}
