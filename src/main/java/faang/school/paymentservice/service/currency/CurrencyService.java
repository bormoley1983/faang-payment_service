package faang.school.paymentservice.service.currency;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import faang.school.paymentservice.dto.Currency;
import faang.school.paymentservice.dto.CurrencyRateResponse;
import faang.school.paymentservice.exception.CurrencyProviderException;
import faang.school.paymentservice.exception.InvalidCurrencyRateResponseException;
import faang.school.paymentservice.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Instant;
import java.util.Map;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static faang.school.paymentservice.service.currency.CurrencyLogMessages.FETCH_FAILURE;
import static faang.school.paymentservice.service.currency.CurrencyLogMessages.FETCH_SUCCESS;
import static faang.school.paymentservice.service.currency.CurrencyLogMessages.STORE_FAILURE;
import static faang.school.paymentservice.service.currency.CurrencyLogMessages.STORE_SUCCESS;
import static faang.school.paymentservice.service.currency.CurrencyLogMessages.UPDATE_FAILURE;
import static faang.school.paymentservice.service.currency.CurrencyLogMessages.UPDATE_SUCCESS;

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
                .map(this::validateResponse)
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
                .switchIfEmpty(Mono.error(new InvalidCurrencyRateResponseException(
                        "Exchange-rate provider returned an empty body")))
                .retryWhen(Retry.backoff(config.getConnectionRetryAttempts(),
                                config.getConnectionRetryDelay())
                        .maxBackoff(config.getConnectionRetryMaxDelay())
                        .jitter(config.getConnectionRetryJitter())
                        .filter(this::isTransientFailure)
                        .onRetryExhaustedThrow((retrySpec, signal) -> signal.failure()))
                .onErrorMap(this::isProviderFailure, this::toSafeProviderException)
                .doOnError(error -> log.error(FETCH_FAILURE, error))
                .doOnSuccess(r -> log.info(FETCH_SUCCESS));
    }

    private CurrencyRateResponse validateResponse(CurrencyRateResponse response) {
        if (!response.isSuccess()) {
            throw new InvalidCurrencyRateResponseException(
                    "Exchange-rate provider reported an unsuccessful response");
        }

        Map<Currency, Double> rates = response.getRates();
        if (rates == null || rates.isEmpty()) {
            throw new InvalidCurrencyRateResponseException(
                    "Exchange-rate provider returned no rates");
        }

        boolean containsInvalidRate = rates.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null
                        || entry.getValue() == null
                        || !Double.isFinite(entry.getValue())
                        || entry.getValue() <= 0);
        if (containsInvalidRate) {
            throw new InvalidCurrencyRateResponseException(
                    "Exchange-rate provider returned an invalid rate");
        }
        return response;
    }

    private boolean isTransientFailure(Throwable error) {
        if (error instanceof WebClientRequestException) {
            return true;
        }
        return error instanceof WebClientResponseException responseException
                && (responseException.getStatusCode().is5xxServerError()
                || responseException.getStatusCode().value() == 429);
    }

    private boolean isProviderFailure(Throwable error) {
        return error instanceof WebClientRequestException
                || error instanceof WebClientResponseException;
    }

    private CurrencyProviderException toSafeProviderException(Throwable error) {
        if (error instanceof WebClientResponseException responseException) {
            return new CurrencyProviderException(
                    "Exchange-rate provider returned HTTP " + responseException.getStatusCode().value());
        }
        return new CurrencyProviderException("Exchange-rate provider is unavailable");
    }

    private Mono<Void> storeExchangeRates(CurrencyRateResponse response) {
        return Flux.fromIterable(response.getRates().entrySet())
                .flatMap(entry -> {
                    Currency currency = entry.getKey();
                    Double rate = entry.getValue();
                    return currencyRateRepository.save(currency, rate, config.getCacheTtl());
                })
                .then()
                .then(currencyRateRepository.markRefreshSuccessful(Instant.now(), config.getCacheTtl()))
                .doOnError(error -> log.error(STORE_FAILURE, error))
                .doOnSuccess(r -> log.info(STORE_SUCCESS));
    }
}
