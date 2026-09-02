package faang.school.paymentservice;

import faang.school.paymentservice.controller.GlobalExceptionHandler;
import faang.school.paymentservice.dto.ErrorResponse;
import faang.school.paymentservice.service.id.UuidCreatorV7Generator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final UuidCreatorV7Generator uuidGenerator = new UuidCreatorV7Generator();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(uuidGenerator);

    @Test
    void errorResponseCopiesDetailsAndReturnsAnImmutableMap() {
        Map<String, String> details = new HashMap<>(Map.of("field", "message"));

        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR", "invalid", Instant.EPOCH, "/payments", "request-1", details);
        details.put("other", "changed");

        assertThat(response.details()).containsExactlyEntriesOf(Map.of("field", "message"));
        assertThatThrownBy(() -> response.details().put("other", "changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void runtimeExceptionNeverLeaksInternalOrNullMessage() {
        // Arrange: request without a servlet request id, exception with null message
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment");

        // Act: invoke the fallback handler
        ErrorResponse response = handler.handleRuntimeException(
                new RuntimeException((String) null), request);

        // Assert: stable payload, generated v7 correlation id, no internal detail
        assertThat(response.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.message()).isEqualTo("An unexpected error occurred");
        assertThat(response.path()).isEqualTo("/api/payment");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.details()).isEmpty();
        assertThat(UUID.fromString(response.correlationId()).version()).isEqualTo(7);
    }

    @Test
    void runtimeException_reusesServletRequestIdWhenPresent() {
        // Arrange: servlet already assigned a request id (MockHttpServletRequest has no setter)
        MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payment");
        when(request.getRequestId()).thenReturn("servlet-req-1");

        // Act: invoke the fallback handler
        ErrorResponse response = handler.handleRuntimeException(
                new IllegalStateException("boom"), request);

        // Assert: correlation id comes from the servlet, not the generator
        assertThat(response.correlationId()).isEqualTo("servlet-req-1");
    }

    @Test
    void validationError_mapsFieldErrorsToDetailsAndKeepsFirstOnCollision() {
        // Arrange: two field errors plus a duplicate field key
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError(
                "target", "paymentNumber", 0L, true, null, null, "paymentNumber must be positive"));
        bindingResult.addError(new FieldError(
                "target", "amount", null, true, null, null, "amount must be at least 0.01"));
        bindingResult.addError(new FieldError(
                "target", "paymentNumber", 1L, true, null, null, "duplicate message ignored"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null, bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment");

        // Act: invoke the validation handler
        ErrorResponse response = handler.handleMethodArgumentNotValidException(exception, request);

        // Assert: stable envelope, per-field details, first message wins on key collision
        assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.message()).isEqualTo("Request validation failed");
        assertThat(response.path()).isEqualTo("/api/payment");
        assertThat(response.details())
                .containsEntry("paymentNumber", "paymentNumber must be positive")
                .containsEntry("amount", "amount must be at least 0.01");
    }

    @Test
    void validationError_nullDefaultMessageFallsBackToInvalidValue() {
        // Arrange: field error whose default message is null
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError(
                "target", "currency", null, true, null, null, null));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                null, bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment");

        // Act: invoke the validation handler
        ErrorResponse response = handler.handleMethodArgumentNotValidException(exception, request);

        // Assert: null message is replaced by the stable fallback text
        assertThat(response.details()).containsEntry("currency", "Invalid value");
    }

    @Test
    void malformedRequest_returnsStableEnvelopeWithoutCauseDetails() {
        // Arrange: Jackson-style parse failure with a detailed cause message
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `Currency` from \"NOT_A_CURRENCY\"",
                null,
                mock(HttpInputMessage.class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment");

        // Act: invoke the malformed-body handler
        ErrorResponse response = handler.handleHttpMessageNotReadableException(exception, request);

        // Assert: no Jackson internals leak into the payload
        assertThat(response.code()).isEqualTo("MALFORMED_REQUEST");
        assertThat(response.message())
                .isEqualTo("Request body is malformed or contains an unsupported value");
        assertThat(response.path()).isEqualTo("/api/payment");
        assertThat(response.details()).isEmpty();
        assertThat(response.correlationId()).isNotBlank();
    }
}
