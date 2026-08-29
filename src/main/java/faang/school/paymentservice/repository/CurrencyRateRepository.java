package faang.school.paymentservice.repository;

import faang.school.paymentservice.dto.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Repository
public class CurrencyRateRepository {
    private static final String KEY_PREFIX = "payment:fx-rate:v1:";
    private static final String LAST_SUCCESS_KEY = KEY_PREFIX + "last-success";

    private final ReactiveStringRedisTemplate currencyRedisTemplate;

    public Mono<Void> save(Currency currency, double value, Duration ttl) {
        return currencyRedisTemplate.opsForValue()
                .set(rateKey(currency), Double.toString(value), ttl)
                .then();
    }

    public Mono<Double> get(Currency currency) {
        return currencyRedisTemplate.hasKey(LAST_SUCCESS_KEY)
                .filter(Boolean.TRUE::equals)
                .flatMap(ignored -> currencyRedisTemplate.opsForValue().get(rateKey(currency)))
                .map(Double::parseDouble);
    }

    public Mono<Void> markRefreshSuccessful(Instant refreshedAt, Duration ttl) {
        return currencyRedisTemplate.opsForValue()
                .set(LAST_SUCCESS_KEY, refreshedAt.toString(), ttl)
                .then();
    }

    public Mono<Instant> getLastSuccessfulRefresh() {
        return currencyRedisTemplate.opsForValue()
                .get(LAST_SUCCESS_KEY)
                .map(Instant::parse);
    }

    private String rateKey(Currency currency) {
        return KEY_PREFIX + currency.name();
    }
}
