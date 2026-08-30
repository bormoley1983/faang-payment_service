package faang.school.paymentservice;

import faang.school.paymentservice.controller.PaymentController;
import faang.school.paymentservice.dto.PaymentRequest;
import faang.school.paymentservice.dto.PaymentResponse;
import faang.school.paymentservice.dto.PaymentStatus;
import faang.school.paymentservice.service.payment.MockVerificationCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private MockVerificationCodeGenerator verificationCodeGenerator;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void sendPayment_returnsSuccessEnvelopeWithGeneratedCode() {
        // Arrange: fixed generator output so the response is deterministic
        when(verificationCodeGenerator.generate()).thenReturn(4242);
        PaymentRequest request = new PaymentRequest(
                42L, new BigDecimal("10.00"), faang.school.paymentservice.dto.Currency.USD);

        // Act: invoke one behavior
        ResponseEntity<PaymentResponse> actual = paymentController.sendPayment(request);

        // Assert: status, echoed fields, and the generated code are all observable
        assertThat(actual.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        PaymentResponse body = actual.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(body.verificationCode()).isEqualTo(4242);
        assertThat(body.paymentNumber()).isEqualTo(42L);
        assertThat(body.amount()).isEqualByComparingTo("10.00");
        assertThat(body.currency()).isEqualTo(faang.school.paymentservice.dto.Currency.USD);
        assertThat(body.message()).isEqualTo("Mock payment accepted");
        verify(verificationCodeGenerator).generate();
    }

    @Test
    void sendPayment_usesFreshCodePerRequest() {
        // Arrange: two distinct codes for two calls
        when(verificationCodeGenerator.generate())
                .thenReturn(1000)
                .thenReturn(9999);
        PaymentRequest request = new PaymentRequest(
                7L, BigDecimal.ONE, faang.school.paymentservice.dto.Currency.EUR);

        // Act: two independent invocations
        int firstCode = paymentController.sendPayment(request).getBody().verificationCode();
        int secondCode = paymentController.sendPayment(request).getBody().verificationCode();

        // Assert: each response carries its own generated code
        assertThat(firstCode).isEqualTo(1000);
        assertThat(secondCode).isEqualTo(9999);
    }
}
