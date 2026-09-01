package faang.school.paymentservice.config.currency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;

public class CurrencyExchangeEnabledEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "currencyExchangeEnabledMirror";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty("currency-exchange.enabled", Boolean.class, false);
        environment.getPropertySources().addLast(new MapPropertySource(
                PROPERTY_SOURCE_NAME,
                Collections.singletonMap("currency-exchange.api.enabled", enabled)));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
