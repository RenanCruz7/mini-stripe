package account.account.service.domain.dto;

import java.math.BigDecimal;

public record TransactionRequest(
        BigDecimal amount
) {
}
