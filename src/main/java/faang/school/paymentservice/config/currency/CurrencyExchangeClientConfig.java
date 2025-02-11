package faang.school.paymentservice.config.currency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class CurrencyExchangeClientConfig {

    @Value("${currency-exchange.api.url}")
    private String url;

    @Bean
    public WebClient CurrencyExchangeClient() {
        return WebClient.builder()
                .baseUrl(url)
                .build();
    }
}
