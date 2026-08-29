package faang.school.paymentservice;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import faang.school.paymentservice.service.currency.CurrencyRateFetcher;
import faang.school.paymentservice.service.currency.CurrencyService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyRateFetcherTest {

    @Mock
    private CurrencyService currencyService;

    @Mock
    private CurrencyExchangeConfig config;

    private SimpleMeterRegistry meterRegistry;
    private ExecutorService executor;
    private CurrencyRateFetcher fetcher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        executor = Executors.newSingleThreadExecutor();
        fetcher = new CurrencyRateFetcher(currencyService, config, meterRegistry);
        when(config.getRefreshTimeout()).thenReturn(Duration.ofSeconds(2));
    }

    @Test
    void waitsForSuccessfulRefreshAndRecordsSuccess() {
        when(currencyService.updateCurrencyRates()).thenReturn(Mono.empty());

        fetcher.fetchCurrencyRates();

        assertThat(meterRegistry.counter("payment.fx.refresh.success").count()).isEqualTo(1);
        assertThat(meterRegistry.get("payment.fx.refresh.last.success.epoch").gauge().value())
                .isPositive();
    }

    @Test
    void propagatesRefreshFailureAndRecordsIt() {
        when(currencyService.updateCurrencyRates()).thenReturn(Mono.error(new IllegalStateException("down")));

        assertThatThrownBy(fetcher::fetchCurrencyRates)
                .isInstanceOf(IllegalStateException.class);

        assertThat(meterRegistry.counter("payment.fx.refresh.failure").count()).isEqualTo(1);
    }

    @Test
    void skipsOverlappingRefresh() throws Exception {
        CountDownLatch subscribed = new CountDownLatch(1);
        when(currencyService.updateCurrencyRates()).thenReturn(Mono.defer(() -> {
            subscribed.countDown();
            return Mono.delay(Duration.ofMillis(200)).then();
        }));

        Future<?> firstRun = executor.submit(fetcher::fetchCurrencyRates);
        assertThat(subscribed.await(1, TimeUnit.SECONDS)).isTrue();

        fetcher.fetchCurrencyRates();
        firstRun.get(1, TimeUnit.SECONDS);

        assertThat(meterRegistry.counter(
                "payment.fx.refresh.skipped", "reason", "overlap").count()).isEqualTo(1);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        meterRegistry.close();
    }
}
