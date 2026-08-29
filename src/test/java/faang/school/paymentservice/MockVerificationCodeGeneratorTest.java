package faang.school.paymentservice;

import faang.school.paymentservice.service.payment.MockVerificationCodeGenerator;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MockVerificationCodeGeneratorTest {

    private final MockVerificationCodeGenerator generator = new MockVerificationCodeGenerator();

    @Test
    void generatesFourDigitCodes() {
        IntStream.range(0, 1_000)
                .map(ignored -> generator.generate())
                .forEach(code -> assertThat(code).isBetween(1_000, 9_999));
    }
}
