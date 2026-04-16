package auth.auth.service.config;

import auth.auth.service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        // Se não tem header ou não começa com "Bearer ", pula o filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Authorization header ausente ou inválido");
            filterChain.doFilter(request, response);
            return;
        }

        // Extrai o token removendo o prefixo "Bearer "
        final String jwt = authHeader.substring(7);

        // Valida se o token não está vazio
        if (jwt.isBlank()) {
            log.warn("Token JWT vazio ou inválido");
            filterChain.doFilter(request, response);
            return;
        }

        // Valida se o token tem a estrutura correta (deve ter 2 pontos)
        if (!jwt.contains(".") || jwt.split("\\.").length != 3) {
            log.warn("Token JWT com estrutura inválida. Esperado: header.payload.signature");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenValid(jwt)) {
                final String username = jwtService.extractUsername(jwt);

                log.debug("JWT validado com sucesso para username: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
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
