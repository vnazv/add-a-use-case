package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a bearer token whose user is deactivated or gone, on every request (FR-SEC-active-account).
 *
 * <p>Login already refuses a disabled account ({@code UserService.loadUserByUsername}), but a JWT issued before
 * deactivation stays valid for its whole lifetime on its own. This validator closes that window
 * (UC-STU-deactivate-student, extension 6b): a failure surfaces as an invalid bearer token, i.e. a 401.
 * Cost: one user lookup per authenticated request.
 */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "The account for this token is deactivated or no longer exists.", null);

    private final UserRepository userRepository;

    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (!(claim instanceof Number userId)) {
            return OAuth2TokenValidatorResult.failure(INACTIVE);
        }
        boolean active = this.userRepository.findById(userId.intValue())
                .map(user -> user.isEnabled())
                .orElse(false);
        return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INACTIVE);
    }

}
