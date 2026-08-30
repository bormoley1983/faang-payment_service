package faang.school.paymentservice;

import faang.school.paymentservice.config.currency.CurrencyExchangeConfig;
import faang.school.paymentservice.dto.Currency;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyExchangeConfigTest {

    private CurrencyExchangeConfig validConfig() {
        CurrencyExchangeConfig config = new CurrencyExchangeConfig();
        config.setUrl("https://api.example.com/v1");
        config.setAccessKey("test-key");
        config.setBase(Currency.EUR);
        config.setConnectTimeout(Duration.ofSeconds(3));
        config.setRequestTimeout(Duration.ofSeconds(10));
        config.setConnectionRetryDelay(Duration.ofSeconds(1));
        config.setConnectionRetryMaxDelay(Duration.ofSeconds(10));
        config.setConnectionRetryAttempts(3);
        config.setConnectionRetryJitter(0.5);
        config.setCacheTtl(Duration.ofHours(13));
        config.setRefreshTimeout(Duration.ofMinutes(2));
        return config;
    }

    @Test
    void validConfiguration_passesDurationInvariant() {
        // Arrange: all durations positive, max delay >= delay
        CurrencyExchangeConfig config = validConfig();

        // Act: evaluate the @AssertTrue invariant
        boolean actual = config.isDurationConfigurationValid();

        // Assert: configuration is accepted
        assertThat(actual).isTrue();
    }

    @Test
    void zeroDuration_failsInvariant() {
        // Arrange: one duration set to zero
        CurrencyExchangeConfig config = validConfig();
        config.setCacheTtl(Duration.ZERO);

        // Act: evaluate the invariant
        boolean actual = config.isDurationConfigurationValid();

        // Assert: zero is rejected as non-positive
        assertThat(actual).isFalse();
    }

    @Test
    void negativeDuration_failsInvariant() {
        // Arrange: one duration set to a negative value
        CurrencyExchangeConfig config = validConfig();
        config.setRefreshTimeout(Duration.ofSeconds(-1));

        // Act: evaluate the invariant
        boolean actual = config.isDurationConfigurationValid();

        // Assert: negative values are rejected
        assertThat(actual).isFalse();
    }

    @Test
    void nullDuration_failsInvariant() {
        // Arrange: one duration left unset
        CurrencyExchangeConfig config = validConfig();
        config.setConnectTimeout(null);

        // Act: evaluate the invariant
        boolean actual = config.isDurationConfigurationValid();

        // Assert: null is rejected as non-positive
        assertThat(actual).isFalse();
    }

    @Test
    void maxRetryDelayShorterThanRetryDelay_failsInvariant() {
        // Arrange: max backoff shorter than the base delay
        CurrencyExchangeConfig config = validConfig();
        config.setConnectionRetryMaxDelay(Duration.ofMillis(500));
        config.setConnectionRetryDelay(Duration.ofSeconds(1));

        // Act: evaluate the invariant
        boolean actual = config.isDurationConfigurationValid();

        // Assert: ordering constraint is enforced
        assertThat(actual).isFalse();
    }

    @Test
    void maxRetryDelayEqualToRetryDelay_passesInvariant() {
        // Arrange: boundary — max backoff exactly equals the base delay
        CurrencyExchangeConfig config = validConfig();
        config.setConnectionRetryMaxDelay(Duration.ofSeconds(1));
        config.setConnectionRetryDelay(Duration.ofSeconds(1));

        // Act: evaluate the invariant
        boolean actual = config.isDurationConfigurationValid();

        // Assert: equality is allowed (>= comparison)
        assertThat(actual).isTrue();
    }
}
