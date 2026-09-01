package faang.school.paymentservice;

import faang.school.paymentservice.service.currency.CurrencyRateFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyExchangeDisabledConfigurationTest {

    @Test
    void scheduledFetcherIsAbsentWhenExternalIntegrationIsDisabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(CurrencyRateFetcher.class)
                .withPropertyValues("currency-exchange.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CurrencyRateFetcher.class));
    }
}
