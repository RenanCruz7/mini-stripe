package auth.auth.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token não pode ser vazio") String refreshToken
) {}

