package com.holo.oshi.vector.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer

/**
 * Vector Service용 Redis 설정
 * 모놀리식과 동일한 Redis 구성
 */
@Configuration
class RedisConfig {

    /**
     * ReactiveRedisTemplate 빈 생성
     * Vector Service 캐싱용
     */
    @Bean
    fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Any> {
        val context = RedisSerializationContext
            .newSerializationContext<String, Any>()
            .key(StringRedisSerializer())
            .hashKey(StringRedisSerializer()) 
            .value(GenericJackson2JsonRedisSerializer())
            .hashValue(GenericJackson2JsonRedisSerializer())
            .build()
        
        return ReactiveRedisTemplate(factory, context)
    }
}