package com.zzz.zerocode.ai.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** 多个 OpenAI 兼容模型通过配置名称注册，各类 Agent 选择自己的模型配置。 */
@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private boolean enabled = false;
    private String codeGenerationModel = "code-generation";
    private int maxMessages = 50;
    private int maxToolCallingRoundTrips = 10;
    private String outputRoot = "./output/generated";
    private long maxFileBytes = 1048576;
    private long agentCacheMaximumSize = 1000;
    private Duration agentCacheExpireAfterAccess = Duration.ofMinutes(10);
    private Duration agentCacheExpireAfterWrite = Duration.ofMinutes(30);
    private Map<String, ModelConfig> models = new LinkedHashMap<>();

    @Data
    public static class ModelConfig {
        private String baseUrl;
        private String modelName;
        @ToString.Exclude
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(120);
        private Integer maxOutputTokens = 16384;
        private Double temperature;
    }
}
