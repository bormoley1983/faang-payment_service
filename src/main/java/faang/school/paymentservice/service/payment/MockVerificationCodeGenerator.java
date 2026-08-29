package faang.school.paymentservice.service.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@Profile("payment-mock")
public class MockVerificationCodeGenerator {

    private static final int MINIMUM_CODE = 1_000;
    private static final int MAXIMUM_CODE_EXCLUSIVE = 10_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public int generate() {
        return secureRandom.nextInt(MINIMUM_CODE, MAXIMUM_CODE_EXCLUSIVE);
    }
}
