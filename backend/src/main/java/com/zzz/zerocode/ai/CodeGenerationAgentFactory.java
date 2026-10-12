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
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.*;
import com.zzz.zerocode.exception.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

/** 按用户、应用和模式缓存 Agent，工具及记忆绑定同一上下文。 */
@Component
public class CodeGenerationAgentFactory {
    private static final ObjectMapper TOOL_JSON = new ObjectMapper();
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
            try {
                var stored = memoryStore.getMessages(context.memoryId());
                var before = recoverCompletedMemory(stored);
                if (!before.equals(stored)) memoryStore.updateMessages(context.memoryId(), before);
                try { return agent.generate(description); }
                catch (RuntimeException failure) {
                    boolean invalidArguments = false;
                    try { invalidArguments = hasInvalidToolArguments(memoryStore.getMessages(context.memoryId())); }
                    catch (RuntimeException inspectionFailure) { failure.addSuppressed(inspectionFailure); }
                    // 工具参数解析或模型请求失败时，不能保留未完成的 tool_calls。
                    try { memoryStore.updateMessages(context.memoryId(), before); }
                    catch (RuntimeException recoveryFailure) { failure.addSuppressed(recoveryFailure); }
                    if (invalidArguments)
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "模型生成的文件内容过长或工具参数不完整，请重试或精简需求");
                    throw failure;
                }
            }
            finally { lock.unlock(); }
        };
    }
    /** 对旧版本留下的残缺工具调用，恢复到最近一个完整回答，保留此前成功的记忆。 */
    private List<ChatMessage> recoverCompletedMemory(List<ChatMessage> messages) {
        int completed = 0;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message instanceof SystemMessage && i == 0) completed = 1;
            if (!(message instanceof AiMessage ai)) continue;
            if (!ai.hasToolExecutionRequests()) { completed = i + 1; continue; }
            Set<String> pending = new HashSet<>();
            for (var call : ai.toolExecutionRequests()) {
                if (!validArguments(call.arguments())) return List.copyOf(messages.subList(0, completed));
                pending.add(call.id());
            }
            int j = i + 1;
            while (j < messages.size() && messages.get(j) instanceof ToolExecutionResultMessage result) {
                pending.remove(result.id()); j++;
            }
            if (!pending.isEmpty()) return List.copyOf(messages.subList(0, completed));
        }
        return List.copyOf(messages);
    }
    private boolean hasInvalidToolArguments(List<ChatMessage> messages) {
        return messages.stream().filter(AiMessage.class::isInstance).map(AiMessage.class::cast)
                .filter(AiMessage::hasToolExecutionRequests).flatMap(ai -> ai.toolExecutionRequests().stream()).anyMatch(call -> !validArguments(call.arguments()));
    }
    private boolean validArguments(String arguments) {
        try { return arguments != null && TOOL_JSON.readTree(arguments).isObject(); }
        catch (Exception ignored) { return false; }
    }

}
