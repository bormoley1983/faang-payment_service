package faang.school.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "currency-exchange.api.access-key=test-only"
})
@AutoConfigureMockMvc
class PaymentEndpointDisabledByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paymentEndpointIsNotExposedWithoutMockProfile() throws Exception {
        mockMvc.perform(post("/api/payment")
                        .contentType("application/json")
                        .content("""
                                {
                                  "paymentNumber": 42,
                                  "amount": 10.00,
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isNotFound());
    }
}
