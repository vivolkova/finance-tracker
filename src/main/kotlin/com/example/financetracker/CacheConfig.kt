package com.example.financetracker

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
class CacheConfig(
    private val objectMapper: ObjectMapper
) {
    @Bean
    fun cacheConfiguration(): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer<Any>(
                    GenericJacksonJsonRedisSerializer(objectMapper)
                )
            )
}
