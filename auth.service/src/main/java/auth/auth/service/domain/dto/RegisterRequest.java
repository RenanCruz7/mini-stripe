package auth.auth.service.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "A senha deve conter no mínimo 8 caracteres, incluindo maiúscula, minúscula, número e caractere especial (@$!%*?&)"
        ) String password
) {}
