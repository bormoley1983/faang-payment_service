package faang.school.paymentservice.repository;

import faang.school.paymentservice.dto.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Repository
public class CurrencyRateRepository {
    private final ReactiveRedisTemplate<String, Double> currencyRedisTemplate;

    public Mono<Void> save(Currency currency, Double value) {
        return currencyRedisTemplate.opsForValue().set(currency.toString(), value).then();
    }

    public Mono<Double> get(Currency currency) {
        return currencyRedisTemplate.opsForValue().get(currency.toString());
    }
}
