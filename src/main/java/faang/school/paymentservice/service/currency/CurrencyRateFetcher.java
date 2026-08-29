package faang.school.paymentservice.service.currency;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class CurrencyRateFetcher {
    private final CurrencyService currencyService;
    private final CurrencyExchangeConfig config;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean refreshRunning = new AtomicBoolean();
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter overlapCounter;

    public CurrencyRateFetcher(
            CurrencyService currencyService,
            CurrencyExchangeConfig config,
            MeterRegistry meterRegistry
    ) {
        this.currencyService = currencyService;
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.successCounter = meterRegistry.counter("payment.fx.refresh.success");
        this.failureCounter = meterRegistry.counter("payment.fx.refresh.failure");
        this.overlapCounter = meterRegistry.counter("payment.fx.refresh.skipped", "reason", "overlap");
        Gauge.builder("payment.fx.refresh.last.success.epoch", lastSuccessEpochSeconds, AtomicLong::get)
                .register(meterRegistry);
    }

    @Scheduled(cron = "${currency-exchange.cron-expression}")
    public void fetchCurrencyRates() {
        if (!refreshRunning.compareAndSet(false, true)) {
            overlapCounter.increment();
            log.warn("Skipped overlapping currency-rate refresh");
            return;
        }

        Timer.Sample timer = Timer.start(meterRegistry);
        try {
            currencyService.updateCurrencyRates().block(config.getRefreshTimeout());
            lastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
            successCounter.increment();
        } catch (RuntimeException exception) {
            failureCounter.increment();
            log.error("Currency-rate refresh failed", exception);
            throw exception;
        } finally {
            timer.stop(meterRegistry.timer("payment.fx.refresh.duration"));
            refreshRunning.set(false);
        }
    }
}
