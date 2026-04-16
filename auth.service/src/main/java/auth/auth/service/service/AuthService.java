package auth.auth.service.service;

import auth.auth.service.domain.dto.AuthResponse;
import auth.auth.service.domain.dto.LoginRequest;
import auth.auth.service.domain.dto.RegisterRequest;
import auth.auth.service.domain.dto.RefreshTokenRequest;
import auth.auth.service.domain.entity.User;
import auth.auth.service.domain.enums.Role;
import auth.auth.service.domain.security.CustomUserDetails;
import auth.auth.service.exception.BusinessException;
import auth.auth.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Tentativa de registro com email duplicado: {}", request.email());
            throw new BusinessException("Email já cadastrado");
        }

        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .active(true)
                .build();

        userRepository.save(user);
        log.info("Novo usuário registrado: {} ({})", user.getEmail(), user.getId());

        var userDetails = new CustomUserDetails(user);
        var accessToken = tokenService.generateToken(userDetails);
        var refreshToken = tokenService.generateRefreshToken(userDetails);

        return AuthResponse.of(accessToken, refreshToken, tokenService.getExpiration(), user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            log.info("Autenticação bem-sucedida para: {}", request.email());
        } catch (Exception ex) {
            log.warn("Falha de autenticação para: {}", request.email());
            throw ex;
        }

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        var userDetails = new CustomUserDetails(user);
        var accessToken = tokenService.generateToken(userDetails);
        var refreshToken = tokenService.generateRefreshToken(userDetails);

        log.info("Login bem-sucedido: {}", user.getEmail());
        return AuthResponse.of(accessToken, refreshToken, tokenService.getExpiration(), user.getId());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!tokenService.isTokenValid(request.refreshToken())) {
            log.warn("Tentativa de refresh com token inválido ou expirado");
            throw new BusinessException("Refresh token inválido ou expirado");
        }

        String email = tokenService.extractUsername(request.refreshToken());
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        var userDetails = new CustomUserDetails(user);
        var accessToken = tokenService.generateToken(userDetails);
        var refreshToken = tokenService.generateRefreshToken(userDetails);

        log.info("Token atualizado para: {}", email);
        return AuthResponse.of(accessToken, refreshToken, tokenService.getExpiration(), user.getId());
    }
}