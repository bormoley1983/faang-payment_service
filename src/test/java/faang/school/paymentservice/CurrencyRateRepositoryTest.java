package faang.school.paymentservice;

import faang.school.paymentservice.dto.Currency;
import faang.school.paymentservice.repository.CurrencyRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyRateRepositoryTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private CurrencyRateRepository repository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new CurrencyRateRepository(redisTemplate);
    }

    @Test
    void savesRateUnderVersionedNamespaceWithTtl() {
        Duration ttl = Duration.ofHours(13);
        when(valueOperations.set("payment:fx-rate:v1:USD", "1.25", ttl))
                .thenReturn(Mono.just(true));

        StepVerifier.create(repository.save(Currency.USD, 1.25, ttl))
                .verifyComplete();

        verify(valueOperations).set("payment:fx-rate:v1:USD", "1.25", ttl);
    }

    @Test
    void doesNotReturnRateWithoutFreshnessMarker() {
        when(redisTemplate.hasKey("payment:fx-rate:v1:last-success"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(repository.get(Currency.USD))
                .verifyComplete();

        verify(valueOperations, never()).get("payment:fx-rate:v1:USD");
    }

    @Test
    void storesLastSuccessfulRefreshWithTtl() {
        Instant refreshedAt = Instant.parse("2026-08-29T12:00:00Z");
        Duration ttl = Duration.ofHours(13);
        when(valueOperations.set(
                "payment:fx-rate:v1:last-success", refreshedAt.toString(), ttl))
                .thenReturn(Mono.just(true));

        StepVerifier.create(repository.markRefreshSuccessful(refreshedAt, ttl))
                .verifyComplete();

        verify(valueOperations).set(
                "payment:fx-rate:v1:last-success", refreshedAt.toString(), ttl);
    }

    @Test
    void returnsLastSuccessfulRefreshWhenPresent() {
        // Arrange: freshness marker holds a parseable instant
        Instant refreshedAt = Instant.parse("2026-08-29T12:00:00Z");
        when(valueOperations.get("payment:fx-rate:v1:last-success"))
                .thenReturn(Mono.just(refreshedAt.toString()));

        // Act: read the marker
        Mono<Instant> actual = repository.getLastSuccessfulRefresh();

        // Assert: stored value round-trips through Instant parsing
        StepVerifier.create(actual)
                .expectNext(refreshedAt)
                .verifyComplete();
    }

    @Test
    void completesEmptyWhenNoFreshnessMarkerStored() {
        // Arrange: marker absent from the cache
        when(valueOperations.get("payment:fx-rate:v1:last-success"))
                .thenReturn(Mono.empty());

        // Act: read the marker
        Mono<Instant> actual = repository.getLastSuccessfulRefresh();

        // Assert: absence surfaces as an empty completion, not an error
        StepVerifier.create(actual)
                .verifyComplete();
    }

    @Test
    void returnsRateWhenFreshnessMarkerPresent() {
        // Arrange: marker present and rate stored under the versioned key
        when(redisTemplate.hasKey("payment:fx-rate:v1:last-success"))
                .thenReturn(Mono.just(true));
        when(valueOperations.get("payment:fx-rate:v1:USD"))
                .thenReturn(Mono.just("1.25"));

        // Act: read the rate
        Mono<Double> actual = repository.get(Currency.USD);

        // Assert: stored string round-trips through double parsing
        StepVerifier.create(actual)
                .expectNext(1.25)
                .verifyComplete();
    }
}
