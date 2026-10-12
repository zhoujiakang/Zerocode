package com.zzz.zerocode.ai.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({AiProperties.class, RedisMemoryProperties.class})
public class RedisChatMemoryConfig {
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore(RedisMemoryProperties properties) {
        var builder = RedisChatMemoryStore.builder()
                .host(properties.getHost()).port(properties.getPort())
                .prefix(properties.getKeyPrefix()).ttl(properties.getTtlSeconds())
                .ssl(properties.isSsl()).storeType(StoreType.STRING);
        if (StringUtils.hasText(properties.getUser())) builder.user(properties.getUser());
        if (StringUtils.hasText(properties.getPassword())) builder.password(properties.getPassword());
        return builder.build();
    }
}
