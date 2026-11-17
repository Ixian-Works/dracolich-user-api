package dm.dracolich.user.web.config;

import dm.dracolich.forge.security.JwtAuthenticationWebFilter;
import dm.dracolich.forge.security.JwtTokenValidator;
import dm.dracolich.user.dto.auth.AuthResponse;
import dm.dracolich.user.dto.auth.LoginRequest;
import dm.dracolich.user.web.controller.AuthController;
import dm.dracolich.user.web.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Since forge 8.1.3 a bad bearer token is a 401. The sign-in endpoints must still answer a player who
 * arrives holding an expired one, and every other path must refuse it.
 */
class StaleTokenTest {

    private static final String BASE = "/dracolich/user/api/v0";
    private static final String STALE = "Bearer header.payload.signature";

    // Every token fails, as an expired one does.
    private final JwtTokenValidator expired = token ->
            Mono.error(new ExpiredJwtException(null, null, "JWT expired"));
    private final JwtAuthenticationWebFilter filter = new SecurityConfig().jwtAuthenticationWebFilter(expired);

    private static MockServerWebExchange underBasePath(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.post(BASE + path)
                .contextPath(BASE)
                .header(HttpHeaders.AUTHORIZATION, STALE));
    }

    private int callsThrough(MockServerWebExchange exchange) {
        AtomicInteger calls = new AtomicInteger();
        WebFilterChain chain = ex -> {
            calls.incrementAndGet();
            return Mono.empty();
        };
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        return calls.get();
    }

    @Test
    void theSignInEndpointsIgnoreAStaleToken_underTheServicesBasePath() {
        for (String path : new String[]{"/auth/login", "/auth/refresh", "/auth/register", "/auth/logout"}) {
            MockServerWebExchange exchange = underBasePath(path);

            assertThat(callsThrough(exchange)).as(path).isEqualTo(1);
            assertThat(exchange.getResponse().getStatusCode()).as(path).isNull();
        }
    }

    @Test
    void aProtectedPathAnswers401ToAStaleToken() {
        MockServerWebExchange exchange = underBasePath("/users/me");

        assertThat(callsThrough(exchange)).isZero();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aPathThatOnlyStartsWithAuthIsNotIgnored() {
        MockServerWebExchange exchange = underBasePath("/authors");

        assertThat(callsThrough(exchange)).isZero();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loggingInWithAStaleTokenReachesTheControllerAndSucceeds() {
        AuthService authService = mock(AuthService.class);
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.just(new AuthResponse("new-access", "new-refresh", 900L)));
        WebTestClient client = WebTestClient.bindToController(new AuthController(authService))
                .webFilter(filter)
                .build();

        client.post().uri("/auth/login")
                .header(HttpHeaders.AUTHORIZATION, STALE)
                .header("Content-Type", "application/json")
                .bodyValue(new LoginRequest("player", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.accessToken").isEqualTo("new-access");
    }
}
