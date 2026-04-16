package payment.payment.service.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @DecimalMax(value = "999999999.99", message = "amount exceeds maximum allowed value")
        BigDecimal amount,

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey
) {
}

