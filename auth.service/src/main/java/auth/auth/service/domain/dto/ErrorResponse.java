package auth.auth.service.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String message,
        List<String> errors,
        String path
) {
    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, message, List.of(), path);
    }

    public static ErrorResponse of(int status, String message, List<String> errors, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, message, errors, path);
    }
}

