package dm.dracolich.user.web.repository;

import dm.dracolich.user.web.entity.ConfirmationSendEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface ConfirmationSendRepository extends ReactiveMongoRepository<ConfirmationSendEntity, String> {
    Mono<Long> countByEmailAndSentAtAfter(String email, Instant since);
}
