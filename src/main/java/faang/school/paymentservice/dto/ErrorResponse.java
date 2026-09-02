package faang.school.paymentservice.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        String correlationId,
        Map<String, String> details
) {
    public ErrorResponse {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
