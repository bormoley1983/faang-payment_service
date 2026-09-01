package faang.school.paymentservice;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the PAY-03 startup invariant end-to-end: when {@code currency-exchange.enabled=true}
 * the context must fail unless a real (non-placeholder) access key is supplied, while the local
 * mock placeholder still starts when the integration is disabled.
 */
class CurrencyExchangeEnabledStartupGuardTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "currency-exchange.api.url=https://api.example.com/v1",
                    "currency-exchange.api.base=EUR",
                    "currency-exchange.api.connect-timeout=PT3S",
                    "currency-exchange.api.request-timeout=PT10S",
                    "currency-exchange.api.connection-retry-delay=PT1S",
                    "currency-exchange.api.connection-retry-max-delay=PT10S",
                    "currency-exchange.api.connection-retry-attempts=3",
                    "currency-exchange.api.connection-retry-jitter=0.5",
                    "currency-exchange.api.cache-ttl=PT13H",
                    "currency-exchange.api.refresh-timeout=PT2M");

    @Test
    void enabledWithMockPlaceholderKey_failsStartup() {
        // Arrange: integration enabled but key is the documented local mock placeholder
        contextRunner
                .withPropertyValues(
                        "currency-exchange.enabled=true",
                        "currency-exchange.api.enabled=true",
                        "currency-exchange.api.access-key=unused-local-mock-key")
                .run(context -> {
                    // Act/Assert: startup must be rejected
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("access-key must not be the documented mock placeholder");
                });
    }

    @Test
    void enabledWithRealKey_startsSuccessfully() {
        // Arrange: integration enabled with a genuine key
        contextRunner
                .withPropertyValues(
                        "currency-exchange.enabled=true",
                        "currency-exchange.api.enabled=true",
                        "currency-exchange.api.access-key=real-production-key")
                .run(context -> {
                    // Act/Assert: startup succeeds and the flag is mirrored into the config
                    assertThat(context).hasNotFailed();
                    CurrencyExchangeConfig config = context.getBean(CurrencyExchangeConfig.class);
                    assertThat(config.isEnabled()).isTrue();
                    assertThat(config.getAccessKey()).isEqualTo("real-production-key");
                });
    }

    @Test
    void disabledWithMockPlaceholderKey_startsSuccessfully() {
        // Arrange: integration disabled, key is the documented local mock placeholder
        contextRunner
                .withPropertyValues(
                        "currency-exchange.enabled=false",
                        "currency-exchange.api.enabled=false",
                        "currency-exchange.api.access-key=unused-local-mock-key")
                .run(context -> {
                    // Act/Assert: local mock startup still works
                    assertThat(context).hasNotFailed();
                    CurrencyExchangeConfig config = context.getBean(CurrencyExchangeConfig.class);
                    assertThat(config.isEnabled()).isFalse();
                });
    }

    @Test
    void disabledWithDefaultKey_doesNotBlockStartup() {
        // Arrange: integration disabled and no explicit key — application.yaml defaults it to "disabled"
        contextRunner
                .withPropertyValues(
                        "currency-exchange.enabled=false",
                        "currency-exchange.api.enabled=false",
                        "currency-exchange.api.access-key=disabled")
                .run(context -> {
                    // Act/Assert: an unset/default key must not block startup when disabled
                    assertThat(context).hasNotFailed();
                    CurrencyExchangeConfig config = context.getBean(CurrencyExchangeConfig.class);
                    assertThat(config.isEnabled()).isFalse();
                    assertThat(config.getAccessKey()).isEqualTo("disabled");
                });
    }

    @Configuration
    @EnableConfigurationProperties(CurrencyExchangeConfig.class)
    static class TestConfig {
    }
}
