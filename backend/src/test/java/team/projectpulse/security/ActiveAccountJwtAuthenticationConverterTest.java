package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import team.projectpulse.user.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * FR-SEC-active-account at the point every bearer-token request passes through.
 */
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtAuthenticationConverterTest {

    @Mock
    UserRepository userRepository;

    JwtAuthenticationConverter delegate;

    ActiveAccountJwtAuthenticationConverter converter;


    @BeforeEach
    void setUp() {
        // The same authorities mapping SecurityConfiguration configures
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("authorities");
        authorities.setAuthorityPrefix("");
        this.delegate = new JwtAuthenticationConverter();
        this.delegate.setJwtGrantedAuthoritiesConverter(authorities);
        this.converter = new ActiveAccountJwtAuthenticationConverter(this.delegate, this.userRepository);
    }

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("j.smith@abc.edu")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("authorities", "ROLE_student");
    }

    @Test
    @DisplayName("An active account converts, with the authorities from the claim unchanged")
    void activeAccountConverts() {
        // A decoded token carries the number as a Long
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(true);

        Jwt jwt = token().claim("userId", 4L).build();

        AbstractAuthenticationToken authentication = this.converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_student")
                .containsExactlyInAnyOrderElementsOf(this.delegate.convert(jwt).getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    @DisplayName("A deactivated account is refused, though the token is otherwise valid")
    void deactivatedAccountIsRefused() {
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(false);

        Throwable thrown = catchThrowable(() -> this.converter.convert(token().claim("userId", 4).build()));

        assertThat(thrown).isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("A token with no userId claim names no account and is refused")
    void tokenWithoutUserIdIsRefused() {
        Throwable thrown = catchThrowable(() -> this.converter.convert(token().build()));

        assertThat(thrown).isInstanceOf(DisabledException.class);
        verifyNoInteractions(this.userRepository);
    }

}
