package dm.dracolich.user.web.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One resent confirmation email, for the limit per address. Removed by a TTL index after a day. */
@Document(collection = "confirmation_sends")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmationSendEntity {
    @Id
    private String id;
    private String email;
    private Instant sentAt;
}
