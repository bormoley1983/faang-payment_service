package faang.school.paymentservice;

import faang.school.paymentservice.controller.GlobalExceptionHandler;
import faang.school.paymentservice.dto.ErrorResponse;
import faang.school.paymentservice.service.id.UuidCreatorV7Generator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            new UuidCreatorV7Generator());

    @Test
    void runtimeExceptionNeverLeaksInternalOrNullMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment");

        ErrorResponse response = handler.handleRuntimeException(
                new RuntimeException((String) null), request);

        assertThat(response.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.message()).isEqualTo("An unexpected error occurred");
        assertThat(response.path()).isEqualTo("/api/payment");
        assertThat(response.correlationId()).isNotBlank();
        assertThat(java.util.UUID.fromString(response.correlationId()).version()).isEqualTo(7);
    }
}
