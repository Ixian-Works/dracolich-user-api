package dm.dracolich.user.web.service;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCodes;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.forge.security.Principal;
import dm.dracolich.forge.security.ReactiveSecurityContextUtil;
import dm.dracolich.user.dto.AccountDto;
import dm.dracolich.user.web.entity.UserEntity;
import dm.dracolich.user.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * A player's own account. The security chain already refuses a request without a valid token; this
 * refuses one without a signed-in player too, so the answer never depends on that alone.
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository users;

    @Override
    public Mono<AccountDto> me() {
        return ReactiveSecurityContextUtil.getPrincipal()
                .filter(Principal::isUser)
                .switchIfEmpty(Mono.error(() -> new ResponseException("Authentication required.",
                        List.of(), HttpStatus.UNAUTHORIZED)))
                .flatMap(principal -> users.findById(principal.id())
                        .switchIfEmpty(Mono.error(() -> notFound(principal.id()))))
                .map(AccountServiceImpl::toAccount);
    }

    static AccountDto toAccount(UserEntity user) {
        return new AccountDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAccountStatus(),
                user.getMetadata() != null ? user.getMetadata().getCreatedAt() : null);
    }

    /** A valid token for an account that no longer exists. */
    private static ResponseException notFound(String id) {
        var code = ErrorCodes.DMD009;
        return new ResponseException(code.format("user", "id", id), List.of(new ApiError(code)), HttpStatus.NOT_FOUND);
    }
}
