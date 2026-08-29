package faang.school.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentRequest(
        @Positive(message = "paymentNumber must be positive")
        long paymentNumber,

        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @NotNull
        BigDecimal amount,

        @NotNull
        Currency currency
) {
}
