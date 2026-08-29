package faang.school.paymentservice.controller;

import faang.school.paymentservice.dto.ErrorResponse;
import faang.school.paymentservice.service.id.UuidV7Generator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final UuidV7Generator uuidV7Generator;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> details = exception.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(
                        error -> error instanceof FieldError fieldError
                                ? fieldError.getField()
                                : "request",
                        error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return error("VALIDATION_ERROR", "Request validation failed", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return error(
                "MALFORMED_REQUEST",
                "Request body is malformed or contains an unsupported value",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException exception, HttpServletRequest request) {
        ErrorResponse response = error(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request,
                Map.of()
        );
        log.error("Unhandled request failure requestId={} path={}",
                response.correlationId(), request.getRequestURI(), exception);
        return response;
    }

    private ErrorResponse error(
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        return new ErrorResponse(
                code,
                message,
                Instant.now(),
                request.getRequestURI(),
                correlationId(request),
                details
        );
    }

    private String correlationId(HttpServletRequest request) {
        return StringUtils.hasText(request.getRequestId())
                ? request.getRequestId()
                : uuidV7Generator.generate().toString();
    }
}
