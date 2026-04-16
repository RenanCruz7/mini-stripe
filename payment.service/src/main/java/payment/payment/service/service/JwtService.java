package payment.payment.service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.warn("Token é nulo ou vazio");
                return false;
            }

            // Valida estrutura do token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Token não contém exatamente 3 partes (header.payload.signature)");
                return false;
            }

            // Tenta parsear o token
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            // Se não expirou
            return !isTokenExpired(token);
        } catch (JwtException ex) {
            log.warn("Erro ao validar JWT: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.error("Erro inesperado ao validar JWT: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (Exception ex) {
            log.error("Erro ao extrair data de expiração do token: {}", ex.getMessage());
            return true; // Considera expirado se houver erro
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Token não pode ser nulo ou vazio");
            }

            final Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claimsResolver.apply(claims);
        } catch (JwtException ex) {
            log.warn("Erro ao extrair claims do token: {}", ex.getMessage());
            throw new IllegalArgumentException("Token JWT inválido: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Erro inesperado ao extrair claims: {}", ex.getMessage());
            throw new IllegalArgumentException("Erro ao processar token JWT", ex);
        }
    }

    private SecretKey getSigningKey() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret não está configurado. Verifique jwt.secret em application.properties");
        }
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

