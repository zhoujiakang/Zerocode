package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.exception.BusinessException;
import com.zzz.zerocode.exception.ErrorCode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 按名称按需初始化模型；不同场景可使用不同配置。创建模型不会发起收费请求。 */
@Component
@RequiredArgsConstructor
public class AiModelRegistry {
    private final AiProperties properties;
    private final ConcurrentMap<String, ChatModel> models = new ConcurrentHashMap<>();

    public ChatModel getModel(String name) {
        if (!properties.isEnabled() || !StringUtils.hasText(name)) throw unavailable();
        return models.computeIfAbsent(name, this::createModel);
    }
    private ChatModel createModel(String name) {
        AiProperties.ModelConfig config = properties.getModels().get(name);
        if (config == null || !StringUtils.hasText(config.getBaseUrl())
                || !StringUtils.hasText(config.getModelName()) || !StringUtils.hasText(config.getApiKey())) throw unavailable();
        URI uri;
        try { uri = URI.create(config.getBaseUrl()); }
        catch (IllegalArgumentException e) { throw unavailable(); }
        if (uri.getHost() == null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) throw unavailable();
        if (config.getTimeout() == null || config.getTimeout().isNegative() || config.getTimeout().isZero()
                || config.getMaxOutputTokens() == null || config.getMaxOutputTokens() <= 0) throw unavailable();
        var builder = OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl()).apiKey(config.getApiKey()).modelName(config.getModelName())
                .timeout(config.getTimeout()).maxTokens(config.getMaxOutputTokens()).maxRetries(1)
                .logRequests(false).logResponses(false);
        if (config.getTemperature() != null) builder.temperature(config.getTemperature());
        return builder.build();
    }
    private BusinessException unavailable() { return new BusinessException(ErrorCode.AI_NOT_CONFIGURED); }
}
