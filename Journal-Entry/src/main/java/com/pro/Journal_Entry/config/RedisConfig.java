//package com.pro.Journal_Entry.config;
//
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.time.Duration;
//
///**
// * Redis Configuration
// *
// * WHY REDIS?
// * Database queries are slow (milliseconds to seconds)
// * Redis is fast (microseconds) because it stores data in RAM
// *
// * CACHE STRATEGY: Read-Through Cache
// * 1. Request comes → Check Redis first
// * 2. If found in Redis (Cache Hit) → Return immediately
// * 3. If not found (Cache Miss) → Query database → Store in Redis → Return
// *
// * TTL (Time To Live): Data expires after 10 minutes
// * This prevents serving stale data
// */
//
//@Configuration
//@EnableCaching //Enables @Cacheable,@CachePut,@CacheEvict
//public class RedisConfig {
///**
// * Redis Template for manual cache operations
// */
//     @Bean
//    public RedisTemplate<String,Object> redisTemplate(
//                RedisConnectionFactory connectionFactory
//     ){
//         RedisTemplate<String,Object> template = new RedisTemplate<>();
//         template.setConnectionFactory(connectionFactory);
//
//         //Key serializer - convert key to String
//         template.setKeySerializer(new StringRedisSerializer());
//         template.setHashKeySerializer(new StringRedisSerializer());
//
//         //value serializer - convert object to JSON
//         template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//         template.setHashKeySerializer(new GenericJackson2JsonRedisSerializer());
//
//         template.afterPropertiesSet();
//         return template;
//     }
//    /**
//     * Cache Manager - Manages all caches
//     */
//
//    @Bean
//    public CacheManager cacheManager(RedisConnectionFactory connectionFactory){
//        //Default cache configuration
//        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(10))
//                .serializeKeysWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(
//                                new StringRedisSerializer()
//                        )
//                )
//                .serializeValuesWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(
//                                new GenericJackson2JsonRedisSerializer()
//                        )
//                )
//                .disableCachingNullValues();// Don't cache null values
//
//        return RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(defaultConfig)
//                .build();
//    }
//
//}
package com.pro.Journal_Entry.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Create custom ObjectMapper with JavaTimeModule
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());  // ← FIX: Support LocalDate
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // Key serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializers with custom ObjectMapper
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);  // ← FIX: Was HashKeySerializer

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Same ObjectMapper configuration
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());  // ← FIX: Support LocalDate
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}
