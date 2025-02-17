package faang.school.paymentservice.service.currecny;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CurrencyRateFetcher {
    private final CurrencyService currencyService;

    @Scheduled(cron = "${currency-exchange.cron-expression}")
    public void CurrencyRateFetch() {
        currencyService.updateCurrencyRates().subscribe();
    }
}