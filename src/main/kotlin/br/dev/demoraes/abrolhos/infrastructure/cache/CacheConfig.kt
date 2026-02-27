package br.dev.demoraes.abrolhos.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.SimpleCacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Spring Cache configuration backed by Redis.
 *
 * Registers named caches for posts with a 7-day TTL. Uses GenericJackson2JsonRedisSerializer for
 * human-readable JSON storage. Implements fail-open error handling: if Redis is unavailable, cache
 * operations are logged and silently skipped so the application falls back to the database.
 */
@Configuration
@EnableCaching
class CacheConfig(private val objectMapper: ObjectMapper) {

        @Bean
        fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
                val mapper = objectMapper.copy()
                mapper.activateDefaultTyping(
                        mapper.polymorphicTypeValidator,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
                )
                val jsonSerializer = GenericJackson2JsonRedisSerializer(mapper)

                val defaultConfig =
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofDays(POST_CACHE_TTL_DAYS))
                                .serializeKeysWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                StringRedisSerializer()
                                        )
                                )
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                jsonSerializer
                                        )
                                )

                return RedisCacheManager.builder(connectionFactory)
                        .cacheDefaults(defaultConfig)
                        .withCacheConfiguration("postBySlug", defaultConfig)
                        .withCacheConfiguration("postSummaries", defaultConfig)
                        .build()
        }

        @Bean
        fun cacheErrorHandler(): CacheErrorHandler =
                object : SimpleCacheErrorHandler() {
                        override fun handleCacheGetError(
                                exception: RuntimeException,
                                cache: Cache,
                                key: Any
                        ) {
                                logger.warn(
                                        "Cache GET error on cache '{}' for key '{}': {}",
                                        cache.name,
                                        key,
                                        exception.message
                                )
                        }

                        override fun handleCachePutError(
                                exception: RuntimeException,
                                cache: Cache,
                                key: Any,
                                value: Any?
                        ) {
                                logger.warn(
                                        "Cache PUT error on cache '{}' for key '{}': {}",
                                        cache.name,
                                        key,
                                        exception.message
                                )
                        }

                        override fun handleCacheEvictError(
                                exception: RuntimeException,
                                cache: Cache,
                                key: Any
                        ) {
                                logger.warn(
                                        "Cache EVICT error on cache '{}' for key '{}': {}",
                                        cache.name,
                                        key,
                                        exception.message
                                )
                        }

                        override fun handleCacheClearError(
                                exception: RuntimeException,
                                cache: Cache
                        ) {
                                logger.warn(
                                        "Cache CLEAR error on cache '{}': {}",
                                        cache.name,
                                        exception.message
                                )
                        }
                }

        companion object {
                private const val POST_CACHE_TTL_DAYS = 7L
                private val logger = LoggerFactory.getLogger(CacheConfig::class.java)
        }
}
