package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.ai.model.AgentContext;
import com.zzz.zerocode.ai.prompt.AgentPromptBuilder;
import com.zzz.zerocode.ai.tools.ToolManager;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import java.util.concurrent.locks.ReentrantLock;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

/** 按用户、应用和模式缓存 Agent，工具及记忆绑定同一上下文。 */
@Component
public class CodeGenerationAgentFactory {
    private final AiModelRegistry modelRegistry;
    private final AiProperties properties;
    private final ChatMemoryStore memoryStore;
    private final AgentPromptBuilder promptBuilder;

    private final ToolManager toolManager;
    private final Cache<AgentContext, CodeGenerationAgent> agentCache;
    // 锁独立于缓存存活，实例过期或重建时同一上下文仍保持串行；锁数量固定。
    private final ReentrantLock[] invocationLocks = new ReentrantLock[256];

    public CodeGenerationAgentFactory(AiModelRegistry modelRegistry, AiProperties properties,
                                      ChatMemoryStore memoryStore, AgentPromptBuilder promptBuilder, ToolManager toolManager) {
        this.modelRegistry = modelRegistry;
        this.properties = properties;
        this.memoryStore = memoryStore;
        this.promptBuilder = promptBuilder;
        this.toolManager = toolManager;
        this.agentCache = Caffeine.newBuilder().maximumSize(properties.getAgentCacheMaximumSize())
                .expireAfterAccess(properties.getAgentCacheExpireAfterAccess())
                .expireAfterWrite(properties.getAgentCacheExpireAfterWrite()).build();
        for (int i = 0; i < invocationLocks.length; i++) invocationLocks[i] = new ReentrantLock();
    }
    public CodeGenerationAgent getAgent(AgentContext context) {
        if (context == null) throw new IllegalArgumentException("应用上下文不能为空");
        return agentCache.get(context, key -> createAgent(key, toolManager.getTools(key)));
    }
    /** 仅移除进程内实例，保留 Redis 对话记忆与应用文件。 */
    public void invalidateAgent(AgentContext context) { agentCache.invalidate(context); }

    CodeGenerationAgent createAgent(AgentContext context, Object... tools) {
        if (context == null || tools == null || tools.length == 0) throw new IllegalArgumentException("必须提供应用上下文和工具集合");
        for (Object tool : tools) if (tool == null) throw new IllegalArgumentException("工具不能为空");
        if (properties.getMaxMessages() < 10 || properties.getMaxToolCallingRoundTrips() < 1)
            throw new IllegalArgumentException("记忆窗口至少 10 条，工具调用轮次必须大于 0");
        var model = modelRegistry.getModel(properties.getCodeGenerationModel());
        var memory = MessageWindowChatMemory.builder().id(context.memoryId())
                .maxMessages(properties.getMaxMessages()).chatMemoryStore(memoryStore).build();
        String prompt = promptBuilder.build(context.mode());
        var agent = AiServices.builder(CodeGenerationAgent.class)
                .chatModel(model).systemMessageProvider(id -> prompt)
                .chatMemory(memory).tools(tools)
                .maxToolCallingRoundTrips(properties.getMaxToolCallingRoundTrips())
                .toolArgumentsErrorHandler(ToolArgumentsErrorHandler.failInvocation())
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocationUnlessVisibleToLlm()).build();
        ReentrantLock lock = invocationLocks[Math.floorMod(context.hashCode(), invocationLocks.length)];
        return description -> {
            lock.lock();
            try { return agent.generate(description); }
            finally { lock.unlock(); }
        };
    }
}
