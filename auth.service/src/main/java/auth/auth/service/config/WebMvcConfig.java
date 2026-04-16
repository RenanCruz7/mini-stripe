package auth.auth.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // Resilience4j rate limiting é aplicado via @RateLimiter nos controllers
    // O RateLimitingInterceptor manual foi substituído por Resilience4j annotations
}
