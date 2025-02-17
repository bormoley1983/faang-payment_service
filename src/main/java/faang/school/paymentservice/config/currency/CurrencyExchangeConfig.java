package faang.school.paymentservice.config.currency;

import faang.school.paymentservice.dto.Currency;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "currency-exchange.api")
@Configuration
public class CurrencyExchangeConfig {
    private String accessKey;
    private Currency base;
    private long connectionRetrySeconds;
    private long connectionRetryAttempts;
}
