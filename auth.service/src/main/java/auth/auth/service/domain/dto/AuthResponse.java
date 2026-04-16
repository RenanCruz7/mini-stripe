package auth.auth.service.domain.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UUID userId
) {
    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, UUID userId) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, userId);
    }
}
