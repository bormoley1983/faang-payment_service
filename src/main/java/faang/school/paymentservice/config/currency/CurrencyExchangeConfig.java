package faang.school.paymentservice.config.currency;

import faang.school.paymentservice.dto.Currency;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "currency-exchange.api")
@Validated
public class CurrencyExchangeConfig {
    @NotBlank
    private String url;

    @NotBlank
    private String accessKey;

    @NotNull
    private Currency base;

    @NotNull
    private Duration connectTimeout;

    @NotNull
    private Duration requestTimeout;

    @NotNull
    private Duration connectionRetryDelay;

    @NotNull
    private Duration connectionRetryMaxDelay;

    @PositiveOrZero
    private long connectionRetryAttempts;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double connectionRetryJitter;

    @NotNull
    private Duration cacheTtl;

    @NotNull
    private Duration refreshTimeout;

    @AssertTrue(message = "currency-exchange durations must be positive and retry max delay must not be shorter than retry delay")
    public boolean isDurationConfigurationValid() {
        return isPositive(connectTimeout)
                && isPositive(requestTimeout)
                && isPositive(connectionRetryDelay)
                && isPositive(connectionRetryMaxDelay)
                && isPositive(cacheTtl)
                && isPositive(refreshTimeout)
                && connectionRetryMaxDelay.compareTo(connectionRetryDelay) >= 0;
    }

    private boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
