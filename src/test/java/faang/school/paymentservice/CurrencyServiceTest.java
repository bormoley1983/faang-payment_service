package faang.school.paymentservice;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import faang.school.paymentservice.dto.Currency;
import faang.school.paymentservice.repository.CurrencyRateRepository;
import faang.school.paymentservice.service.currecny.CurrencyService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"success\": true, \"rates\": {\"USD\": 1.25}}"));

        when(currencyRateRepository.save(Currency.USD, 1.25)).thenReturn(Mono.empty());

        Mono<Void> updateMono = currencyService.updateCurrencyRates();

        StepVerifier.create(updateMono)
                .expectSubscription()
                .verifyComplete();

        verify(currencyRateRepository, times(1)).save(Currency.USD, 1.25);
        mockWebServer.takeRequest();
    }

    @Test
    void testUpdateCurrencyRates_FailureFetch() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{ \"error\": \"Invalid API key\" }"));

        Mono<Void> updateMono = currencyService.updateCurrencyRates();

        StepVerifier.create(updateMono)
                .expectError()
                .verify();

        verify(currencyRateRepository, times(0)).save(any(Currency.class), anyDouble());
        mockWebServer.takeRequest();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }
}