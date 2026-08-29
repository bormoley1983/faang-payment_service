package faang.school.paymentservice.exception;

public class InvalidCurrencyRateResponseException extends RuntimeException {

    public InvalidCurrencyRateResponseException(String message) {
        super(message);
    }
}
