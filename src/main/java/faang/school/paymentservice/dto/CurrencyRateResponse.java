package faang.school.paymentservice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Data
public class CurrencyRateResponse {
    @AssertTrue(message = "Response from API indicates failure: success flag is false")
    private boolean success;

    private long timestamp;

    private Currency base;

    private String date;

    @NotEmpty(message = "Response from API contains no rates")
    private Map<Currency, Double> rates;
}
