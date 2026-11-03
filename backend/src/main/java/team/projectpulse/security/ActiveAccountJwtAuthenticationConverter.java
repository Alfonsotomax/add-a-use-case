package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a bearer token whose account has been deactivated since the token was issued (FR-SEC-active-account).
 *
 * <p>The {@code enabled} flag is checked at login, but a JWT outlives that check by up to its whole lifetime. This
 * converter runs on every bearer-token request, inside {@code BearerTokenAuthenticationFilter}, so the token of a
 * deactivated student or instructor stops working on her next request rather than when it expires. It throws
 * {@link DisabledException}, which the filter turns into a 401 through the bearer-token entry point; the SPA treats a
 * 401 as "log in again".
 *
 * <p>Authorities are left to the wrapped {@link JwtAuthenticationConverter}; this class adds the account check only.
 * A token with no usable {@code userId} claim names no account, and is refused as inactive.
 */
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegate;
    private final UserRepository userRepository;


    public ActiveAccountJwtAuthenticationConverter(JwtAuthenticationConverter delegate, UserRepository userRepository) {
        this.delegate = delegate;
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = readUserId(jwt);
        if (userId == null || !this.userRepository.existsByIdAndEnabledTrue(userId)) {
            throw new DisabledException("This account is deactivated.");
        }
        return this.delegate.convert(jwt);
    }

    /**
     * {@code JwtProvider} writes the claim as an {@code Integer}, but a decoded token returns a JSON number as a
     * {@code Long}, so accept any number.
     */
    private static Integer readUserId(Jwt jwt) {
        Object claim = jwt.getClaims().get("userId");
        if (claim instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

}
