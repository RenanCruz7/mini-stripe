package account.account.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAccountRequest(
        @NotNull UUID userId
) {
}
