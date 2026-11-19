package dm.dracolich.user.dto;

import java.time.Instant;

/**
 * What a signed-in player can read about their own account: never a password hash or a token.
 * camelCase on the wire, as user-api's other DTOs are.
 */
public record AccountDto(
        String id,
        String username,
        String email,
        AccountStatusEnum accountStatus,
        Instant createdAt) { }
