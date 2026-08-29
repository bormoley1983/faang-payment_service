package faang.school.paymentservice.controller;

import faang.school.paymentservice.dto.PaymentRequest;
import jakarta.validation.Valid;
import faang.school.paymentservice.dto.PaymentResponse;
import faang.school.paymentservice.dto.PaymentStatus;
import faang.school.paymentservice.service.payment.MockVerificationCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Profile("payment-mock")
@RequiredArgsConstructor
public class PaymentController {

    private final MockVerificationCodeGenerator verificationCodeGenerator;

    @PostMapping("/payment")
    public ResponseEntity<PaymentResponse> sendPayment(@RequestBody @Valid PaymentRequest dto) {
        int verificationCode = verificationCodeGenerator.generate();

        return ResponseEntity.ok(new PaymentResponse(
                PaymentStatus.SUCCESS,
                verificationCode,
                dto.paymentNumber(),
                dto.amount(),
                dto.currency(),
                "Mock payment accepted")
        );
    }
}
