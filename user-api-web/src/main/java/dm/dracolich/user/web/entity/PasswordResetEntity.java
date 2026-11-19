package dm.dracolich.user.web.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One emailed password reset link. Only the token's SHA-256 hash is stored, as for refresh tokens.
 * Single use ({@code usedAt}), and removed by a TTL index at {@code expiresAt}.
 */
@Document(collection = "password_resets")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetEntity {
    @Id
    private String id;
    private String userId;
    private String tokenHash;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;
}
