package dm.dracolich.user.web.repository;

import dm.dracolich.user.web.entity.PasswordResetEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface PasswordResetRepository extends ReactiveMongoRepository<PasswordResetEntity, String> {
    Mono<PasswordResetEntity> findByTokenHash(String tokenHash);

    /** Whether a link was sent to this account after {@code since}, for the one-a-minute limit. */
    Mono<Boolean> existsByUserIdAndCreatedAtAfter(String userId, Instant since);
}
