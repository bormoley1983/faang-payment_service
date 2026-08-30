package faang.school.paymentservice;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import faang.school.paymentservice.dto.Currency;
import faang.school.paymentservice.repository.CurrencyRateRepository;
import faang.school.paymentservice.service.currency.CurrencyService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class CurrencyServiceTest {

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private CurrencyExchangeConfig config;

    @Mock
    private WebClient currencyExchangeClient;

    @InjectMocks
    private CurrencyService currencyService;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        currencyExchangeClient = WebClient.create(baseUrl);
        currencyService = new CurrencyService(currencyExchangeClient, currencyRateRepository, config);
    }

    @Test
    void testGetExchangeRate_CurrencyExists() {
        Currency currency = Currency.USD;
        Double rate = 1.25;

        when(currencyRateRepository.get(currency)).thenReturn(Mono.just(rate));

        Mono<Double> exchangeRate = currencyService.getExchangeRate(currency);

        StepVerifier.create(exchangeRate)
                .expectNext(rate)
                .verifyComplete();

        verify(currencyRateRepository, times(1)).get(currency);
    }

    @Test
    void testUpdateCurrencyRates_Success() throws InterruptedException {
        Duration cacheTtl = configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": 1.25}}"));

        when(currencyRateRepository.save(Currency.USD, 1.25, cacheTtl)).thenReturn(Mono.empty());
        when(currencyRateRepository.markRefreshSuccessful(any(Instant.class), eq(cacheTtl)))
                .thenReturn(Mono.empty());

        Mono<Void> updateMono = currencyService.updateCurrencyRates();

        StepVerifier.create(updateMono)
                .expectSubscription()
                .verifyComplete();

        verify(currencyRateRepository, times(1)).save(Currency.USD, 1.25, cacheTtl);
        verify(currencyRateRepository, times(1))
                .markRefreshSuccessful(any(Instant.class), eq(cacheTtl));
        mockWebServer.takeRequest();
    }

    @Test
    void testUpdateCurrencyRates_FailureFetch() throws InterruptedException {
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{ \"error\": \"Invalid API key\" }"));

        Mono<Void> updateMono = currencyService.updateCurrencyRates();

        StepVerifier.create(updateMono)
                .expectError()
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
        mockWebServer.takeRequest();
    }

    @Test
    void updateCurrencyRatesRejectsProviderFailureBody() {
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": false, \"rates\": {\"USD\": 1.25}}"));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().contains("unsuccessful"))
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void updateCurrencyRatesRejectsInvalidRate() {
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": -1}}"));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().contains("invalid rate"))
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void permanentClientErrorIsNotRetried() {
        configureExchange(3);
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().equals(
                        "Exchange-rate provider returned HTTP 401"))
                .verify();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void transientServerErrorIsRetriedWithinConfiguredBound() {
        Duration cacheTtl = configureExchange(2);
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": 1.25}}"));
        when(currencyRateRepository.save(Currency.USD, 1.25, cacheTtl)).thenReturn(Mono.empty());
        when(currencyRateRepository.markRefreshSuccessful(any(Instant.class), eq(cacheTtl)))
                .thenReturn(Mono.empty());

        StepVerifier.create(currencyService.updateCurrencyRates())
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void rate429IsRetriedAsTransient() {
        Duration cacheTtl = configureExchange(1);
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": 1.25}}"));
        when(currencyRateRepository.save(Currency.USD, 1.25, cacheTtl)).thenReturn(Mono.empty());
        when(currencyRateRepository.markRefreshSuccessful(any(Instant.class), eq(cacheTtl)))
                .thenReturn(Mono.empty());

        StepVerifier.create(currencyService.updateCurrencyRates())
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void emptyProviderBodyIsRejectedWithoutStore() {
        // An empty JSON object deserializes to success=false (primitive default),
        // so the rejection surfaces as an unsuccessful response, not an empty body.
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().contains("unsuccessful"))
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void nullRatesAreRejected() {
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true }"));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().contains("no rates"))
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void emptyRatesMapIsRejected() {
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {}}"));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().contains("no rates"))
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void nonFiniteRateIsRejectedAtDecodeLayer() {
        // Jackson rejects the NaN token before validation runs; the failure must
        // still surface as an error and never reach the store.
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": 1.25, \"EUR\": NaN}}"));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectError()
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void zeroRateIsRejected() {
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": 0}}"));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().contains("invalid rate"))
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void connectionFailureIsMappedToSafeProviderException() {
        configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage()
                        .equals("Exchange-rate provider is unavailable"))
                .verify();

        verify(currencyRateRepository, times(0))
                .save(any(Currency.class), anyDouble(), any(Duration.class));
    }

    @Test
    void storeFailurePropagatesToCaller() {
        // Note: the production chain uses `.then(...)` after the save flux, which
        // swallows the first error signal; the observable contract is that the
        // failure still propagates to the caller (the scheduled fetcher records it).
        Duration cacheTtl = configureExchange(0);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": 1.25}}"));
        when(currencyRateRepository.save(Currency.USD, 1.25, cacheTtl))
                .thenReturn(Mono.error(new IllegalStateException("redis down")));
        lenient().when(currencyRateRepository.markRefreshSuccessful(any(Instant.class), eq(cacheTtl)))
                .thenReturn(Mono.empty());

        StepVerifier.create(currencyService.updateCurrencyRates())
                .expectErrorMatches(error -> error.getMessage().equals("redis down"))
                .verify();
    }

    @Test
    void getExchangeRate_completesEmptyWhenNoFreshRate() {
        when(currencyRateRepository.get(Currency.USD)).thenReturn(Mono.empty());

        StepVerifier.create(currencyService.getExchangeRate(Currency.USD))
                .verifyComplete();
    }

    private Duration configureExchange(long retryAttempts) {
        Duration cacheTtl = Duration.ofHours(13);
        when(config.getConnectionRetryAttempts()).thenReturn(retryAttempts);
        when(config.getConnectionRetryDelay()).thenReturn(Duration.ofMillis(1));
        when(config.getConnectionRetryMaxDelay()).thenReturn(Duration.ofMillis(5));
        when(config.getConnectionRetryJitter()).thenReturn(0.0);
        lenient().when(config.getCacheTtl()).thenReturn(cacheTtl);
        return cacheTtl;
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }
}
