package dm.dracolich.user.web.controller;

import dm.dracolich.user.dto.AccountDto;
import dm.dracolich.user.web.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("users")
@Tag(name = "Users")
@RequiredArgsConstructor
public class UserController {

    private final AccountService accounts;

    @Operation(summary = "My account", description = "The signed-in player's own account: id, username, email, account status and when it was made. Read from the player in the token; it takes no id, so no player can read another's.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The account",
                    content = @Content(schema = @Schema(implementation = AccountDto.class))),
            @ApiResponse(responseCode = "401", description = "No valid access token"),
            @ApiResponse(responseCode = "404", description = "The token's account no longer exists (DMD009)")
    })
    @GetMapping(path = {"/me"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AccountDto> me() {
        return accounts.me();
    }
}
