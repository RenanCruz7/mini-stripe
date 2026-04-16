package account.account.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.lang.Nullable;

import java.time.Duration;

@Slf4j
@EnableCaching
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        var jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        var config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer)
                );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, @Nullable org.springframework.cache.Cache cache, @Nullable Object key) {
                log.warn("Cache GET error para key: {} no cache: {}", key, cache != null ? cache.getName() : "unknown", exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, @Nullable org.springframework.cache.Cache cache, @Nullable Object key, @Nullable Object value) {
                log.warn("Cache PUT error para key: {} no cache: {}", key, cache != null ? cache.getName() : "unknown", exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, @Nullable org.springframework.cache.Cache cache, @Nullable Object key) {
                log.warn("Cache EVICT error para key: {} no cache: {}", key, cache != null ? cache.getName() : "unknown", exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, @Nullable org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error no cache: {}", cache != null ? cache.getName() : "unknown", exception);
            }
        };
    }
}