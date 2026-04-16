package account.account.service.config;

import account.account.service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Se não tem header ou não começa com "Bearer ", pula o filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Authorization header ausente ou inválido");
            filterChain.doFilter(request, response);
            return;
        }

        // Extrai o token removendo o prefixo "Bearer "
        final String token = authHeader.substring(7);

        // Valida se o token não está vazio
        if (token.isBlank()) {
            log.warn("Token JWT vazio ou inválido");
            filterChain.doFilter(request, response);
            return;
        }

        // Valida se o token tem a estrutura correta (deve ter 2 pontos)
        if (!token.contains(".") || token.split("\\.").length != 3) {
            log.warn("Token JWT com estrutura inválida. Esperado: header.payload.signature");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenValid(token)) {
                var username = jwtService.extractUsername(token);
                var role = jwtService.extractRole(token);

                log.debug("JWT validado com sucesso para username: {}", username);

                var auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Token JWT expirado ou inválido");
            }
        } catch (Exception ex) {
            log.warn("Erro ao validar JWT token: {}", ex.getMessage());
            log.debug("Detalhe do erro: ", ex);
        }

        filterChain.doFilter(request, response);
    }
}