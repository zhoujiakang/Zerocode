package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.*;
import com.zzz.zerocode.ai.model.*;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import redis.clients.jedis.Jedis;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 显式启用真实 Redis 验证；仅写入随机测试命名空间，结束后清理。 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TESTS", matches = "true")
class RedisMemoryIntegrationTest {
    @Test void stringStoragePersistsIsolatedMemoryAndExpires() {
        var properties = new RedisMemoryProperties();
        properties.setHost(System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1"));
        properties.setPort(Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379")));
        properties.setUser(System.getenv("REDIS_USER"));
        properties.setPassword(System.getenv("REDIS_PASSWORD"));
        properties.setKeyPrefix("zerocode:test:" + UUID.randomUUID() + ":");
        properties.setTtlSeconds(60);
        var store = new RedisChatMemoryConfig().redisChatMemoryStore(properties);
        var context = new AgentContext(1, 2, CodeGenType.HTML);
        String key = properties.getKeyPrefix() + context.memoryId();
        try (Jedis jedis = new Jedis(properties.getHost(), properties.getPort())) {
            if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
                if (properties.getUser() == null || properties.getUser().isBlank()) jedis.auth(properties.getPassword());
                else jedis.auth(properties.getUser(), properties.getPassword());
            }
            try {
                var memory = MessageWindowChatMemory.builder().id(context.memoryId()).maxMessages(10).chatMemoryStore(store).build();
                memory.add(UserMessage.from("创建读书网页")); memory.add(AiMessage.from("已完成初稿"));
                var restored = MessageWindowChatMemory.builder().id(context.memoryId()).maxMessages(10).chatMemoryStore(store).build();
                assertEquals(2, restored.messages().size());
                assertEquals("string", jedis.type(key));
                assertTrue(jedis.ttl(key) > 0 && jedis.ttl(key) <= 60);
                assertTrue(store.getMessages(new AgentContext(2, 2, CodeGenType.HTML).memoryId()).isEmpty());
                assertTrue(store.getMessages(new AgentContext(1, 2, CodeGenType.MULTI_FILE).memoryId()).isEmpty());
                restored.clear(); assertFalse(jedis.exists(key));
            } finally { store.deleteMessages(context.memoryId()); }
        }
    }
}
