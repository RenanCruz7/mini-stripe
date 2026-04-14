package auth.auth.service.config;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final int REQUESTS_PER_MINUTE = 5;
    private static final long MINUTE_IN_MS = 60_000L;
    private final Map<String, RateLimitBucket> cache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) {
        String path = request.getRequestURI();

        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            String clientIP = getClientIP(request);
            RateLimitBucket bucket = cache.computeIfAbsent(clientIP, key -> new RateLimitBucket());

            if (bucket.tryConsume()) {
                log.debug("Rate limit OK para IP: {}", clientIP);
                return true;
            } else {
                log.warn("Rate limit excedido para IP: {} - Tentativas: {}", clientIP, bucket.getRequestCount());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                try {
                    response.getWriter().write("{\"error\": \"Muitas requisições. Tente novamente em 1 minuto.\", \"retry_after\": 60}");
                } catch (Exception e) {
                    log.error("Erro ao escrever resposta de rate limit", e);
                }
                return false;
            }
        }

        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Simples token bucket para rate limiting
    private static class RateLimitBucket {
        private long firstRequestTime;
        private int requestCount = 0;

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();

            if (firstRequestTime == 0 || (now - firstRequestTime) > MINUTE_IN_MS) {
                firstRequestTime = now;
                requestCount = 1;
                return true;
            }

            if (requestCount < REQUESTS_PER_MINUTE) {
                requestCount++;
                return true;
            }

            return false;
        }

        synchronized int getRequestCount() {
            return requestCount;
        }
    }
}

