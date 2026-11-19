package dm.dracolich.user.web.controller;

import dm.dracolich.forge.controller.ControllerAdvice;
import dm.dracolich.forge.security.JwtTokenValidator;
import dm.dracolich.user.dto.AccessLevelEnum;
import dm.dracolich.user.dto.AccountStatusEnum;
import dm.dracolich.user.web.config.SecurityConfig;
import dm.dracolich.user.web.entity.MetadataEntity;
import dm.dracolich.user.web.entity.UserEntity;
import dm.dracolich.user.web.repository.UserRepository;
import dm.dracolich.user.web.service.AccountServiceImpl;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GET /users/me through the service's own security chain: a token "good-<id>" is that player's
 * token, any other token is refused, and the answer is always the token's own player. Bound to the
 * controller alone, so a body comes back unwrapped; in the service forge wraps it in the envelope.
 */
class UserControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private WebTestClient client;

    private final JwtTokenValidator validator = token -> token.startsWith("good-")
            ? Mono.just(Jwts.claims().subject(token.substring(5)).add("accessLevel", "COMMON_USER").build())
            : Mono.error(new IllegalArgumentException("bad token"));

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        ObjectMapper mapper = JsonMapper.builder().build();
        SecurityWebFilterChain chain = config.securityFilterChain(ServerHttpSecurity.http(),
                config.jwtAuthenticationWebFilter(validator), mapper);
        client = WebTestClient.bindToController(new UserController(new AccountServiceImpl(users)))
                .controllerAdvice(new ControllerAdvice())
                .webFilter(new WebFilterChainProxy(chain))
                .build();
    }

    private static UserEntity user(String id, String username, String email) {
        return UserEntity.builder()
                .id(id).username(username).email(email)
                .password("$2a$10$hash-that-must-never-leave")
                .accessLevel(AccessLevelEnum.COMMON_USER)
                .accountStatus(AccountStatusEnum.ACTIVE)
                .metadata(MetadataEntity.builder().createdAt(Instant.parse("2026-11-01T10:00:00Z")).build())
                .build();
    }

    @Test
    void aSignedInPlayerReadsTheirOwnAccount() {
        when(users.findById("user-1")).thenReturn(Mono.just(user("user-1", "jace", "jace@example.com")));

        client.get().uri("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer good-user-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("user-1")
                .jsonPath("$.username").isEqualTo("jace")
                .jsonPath("$.email").isEqualTo("jace@example.com")
                .jsonPath("$.accountStatus").isEqualTo("ACTIVE")
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.password").doesNotExist()
                .jsonPath("$.userHash").doesNotExist();
    }

    @Test
    void noTokenIs401AndReadsNothing() {
        client.get().uri("/users/me").exchange().expectStatus().isUnauthorized();

        verify(users, never()).findById(anyString());
    }

    @Test
    void aBadTokenIs401AndReadsNothing() {
        client.get().uri("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer forged")
                .exchange().expectStatus().isUnauthorized();

        verify(users, never()).findById(anyString());
    }

    @Test
    void anIdInTheRequestIsIgnored() {
        when(users.findById("user-1")).thenReturn(Mono.just(user("user-1", "jace", "jace@example.com")));

        client.get().uri("/users/me?id=user-2&userId=user-2").header(HttpHeaders.AUTHORIZATION, "Bearer good-user-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo("user-1");

        verify(users, never()).findById("user-2");
    }

    @Test
    void thereIsNoRouteThatNamesAPlayer() {
        client.get().uri("/users/user-2").header(HttpHeaders.AUTHORIZATION, "Bearer good-user-1")
                .exchange().expectStatus().isNotFound();

        verify(users, never()).findById(anyString());
    }

    @Test
    void aTokenWhoseAccountIsGoneIs404() {
        when(users.findById("user-9")).thenReturn(Mono.empty());

        client.get().uri("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer good-user-9")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.errors[0].error").isEqualTo("DMD009");
    }
}
