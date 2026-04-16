package account.account.service.domain.dto;

import account.account.service.domain.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}