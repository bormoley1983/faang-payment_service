package faang.school.paymentservice.service.currecny;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import faang.school.paymentservice.dto.Currency;
import faang.school.paymentservice.dto.CurrencyRateResponse;
import faang.school.paymentservice.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static faang.school.paymentservice.utils.CurrencyLogMessage.FETCH_FAILURE;
import static faang.school.paymentservice.utils.CurrencyLogMessage.FETCH_SUCCESS;
import static faang.school.paymentservice.utils.CurrencyLogMessage.STORE_FAILURE;
import static faang.school.paymentservice.utils.CurrencyLogMessage.STORE_SUCCESS;
import static faang.school.paymentservice.utils.CurrencyLogMessage.UPDATE_FAILURE;
import static faang.school.paymentservice.utils.CurrencyLogMessage.UPDATE_SUCCESS;

@Slf4j
@Validated
@RequiredArgsConstructor
@Service
public class CurrencyService {
    private final WebClient currencyExchangeClient;
    private final CurrencyRateRepository currencyRateRepository;
    private final CurrencyExchangeConfig config;

    public Mono<Double> getExchangeRate(Currency currency) {
        return currencyRateRepository.get(currency);
    }

    public Mono<Void> updateCurrencyRates() {
        String currencies = EnumSet.allOf(Currency.class).stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        return fetchExchangeRates(currencies)
                .flatMap(this::storeExchangeRates)
                .doOnError(error -> log.error(UPDATE_FAILURE))
                .doOnSuccess(aVoid -> log.info(UPDATE_SUCCESS));
    }

    private Mono<CurrencyRateResponse> fetchExchangeRates(String currencies) {
        return currencyExchangeClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/latest")
                        .queryParam("access_key", config.getAccessKey())
                        .queryParam("base", config.getBase())
                        .queryParam("symbols", currencies)
                        .build())
                .retrieve()
                .bodyToMono(CurrencyRateResponse.class)
                .retryWhen(Retry.fixedDelay(config.getConnectionRetryAttempts(),
                        Duration.ofSeconds(config.getConnectionRetrySeconds())))
                .doOnError(error -> log.error(FETCH_FAILURE, error))
                .doOnSuccess(r -> log.info(FETCH_SUCCESS));
    }

    private Mono<Void> storeExchangeRates(CurrencyRateResponse response) {
        return Flux.fromIterable(response.getRates().entrySet())
                .flatMap(entry -> {
                    Currency currency = entry.getKey();
                    Double rate = entry.getValue();
                    return currencyRateRepository.save(currency, rate);
                })
                .then()
                .doOnError(error -> log.error(STORE_FAILURE, error))
                .doOnSuccess(r -> log.info(STORE_SUCCESS));
    }
}
