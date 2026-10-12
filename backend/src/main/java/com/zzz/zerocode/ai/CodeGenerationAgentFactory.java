package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.ai.model.AgentContext;
import com.zzz.zerocode.ai.prompt.AgentPromptBuilder;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 第一阶段的组装工厂；调用方传入已绑定应用目录的工具集合。 */
@Component
@RequiredArgsConstructor
public class CodeGenerationAgentFactory {
    private final AiModelRegistry modelRegistry;
    private final AiProperties properties;
    private final ChatMemoryStore memoryStore;
    private final AgentPromptBuilder promptBuilder;

    public CodeGenerationAgent createAgent(AgentContext context, Object... tools) {
        if (context == null || tools == null || tools.length == 0) throw new IllegalArgumentException("必须提供应用上下文和工具集合");
        for (Object tool : tools) if (tool == null) throw new IllegalArgumentException("工具不能为空");
        if (properties.getMaxMessages() < 10 || properties.getMaxToolCallingRoundTrips() < 1)
            throw new IllegalArgumentException("记忆窗口至少 10 条，工具调用轮次必须大于 0");
        var model = modelRegistry.getModel(properties.getCodeGenerationModel());
        var memory = MessageWindowChatMemory.builder().id(context.memoryId())
                .maxMessages(properties.getMaxMessages()).chatMemoryStore(memoryStore).build();
        String prompt = promptBuilder.build(context.mode());
        return AiServices.builder(CodeGenerationAgent.class)
                .chatModel(model).systemMessageProvider(id -> prompt)
                .chatMemory(memory).tools(tools)
                .maxToolCallingRoundTrips(properties.getMaxToolCallingRoundTrips()).build();
    }
}
