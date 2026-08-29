package faang.school.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("payment-mock")
class PaymentEndpointMockProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paymentEndpointIsAvailableForExplicitMockProfile() throws Exception {
        mockMvc.perform(post("/api/payment")
                        .contentType("application/json")
                        .content("""
                                {
                                  "paymentNumber": 42,
                                  "amount": 10.00,
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentNumber").value(42));
    }

    @Test
    void rejectsNonPositivePaymentNumberWithSafeValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/payment")
                        .contentType("application/json")
                        .content("""
                                {
                                  "paymentNumber": 0,
                                  "amount": 10.00,
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.paymentNumber")
                        .value("paymentNumber must be positive"));
    }

    @Test
    void malformedCurrencyDoesNotLeakJacksonDetails() throws Exception {
        mockMvc.perform(post("/api/payment")
                        .contentType("application/json")
                        .content("""
                                {
                                  "paymentNumber": 42,
                                  "amount": 10.00,
                                  "currency": "NOT_A_CURRENCY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Request body is malformed or contains an unsupported value"))
                .andExpect(jsonPath("$.path").value("/api/payment"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
