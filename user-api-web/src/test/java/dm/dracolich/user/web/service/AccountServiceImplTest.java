package dm.dracolich.user.web.service;

import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.forge.security.Principal;
import dm.dracolich.user.web.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** The service refuses without a signed-in player even if called without the security chain in front. */
class AccountServiceImplTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AccountServiceImpl service = new AccountServiceImpl(users);

    @Test
    void noPlayerIs401() {
        StepVerifier.create(service.me())
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(ResponseException.class);
                    assertThat(((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();
        verify(users, never()).findById(anyString());
    }

    @Test
    void aGuestPrincipalIs401() {
        StepVerifier.create(service.me().contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        new TestingAuthenticationToken(Principal.anon("anon-1"), null))))
                .expectErrorSatisfies(e -> assertThat(((ResponseException) e).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED))
                .verify();
        verify(users, never()).findById(anyString());
    }
}
