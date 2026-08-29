package faang.school.paymentservice.config.currency;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;


@Configuration
@RequiredArgsConstructor
public class CurrencyExchangeClientConfig {

    private final CurrencyExchangeConfig config;

    @Bean
    public WebClient currencyExchangeClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(config.getConnectTimeout().toMillis()))
                .responseTimeout(config.getRequestTimeout());

        return WebClient.builder()
                .baseUrl(config.getUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
