package dm.dracolich.user.web.service;

import dm.dracolich.user.dto.AccountDto;
import reactor.core.publisher.Mono;

public interface AccountService {
    /** The signed-in player's own account, read from the player in the token, never from a request id. */
    Mono<AccountDto> me();
}
