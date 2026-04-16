package payment.payment.service.domain.dto;

import payment.payment.service.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentEvent(
        UUID paymentId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt
) {
}

