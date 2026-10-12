package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.ai.model.*;
import com.zzz.zerocode.ai.prompt.AgentPromptBuilder;
import com.zzz.zerocode.ai.tools.ToolManager;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import java.nio.file.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CodeGenerationAgentFactoryTest {
    @TempDir Path directory;
    public static class TestFileTool {
        private final Path directory;
        TestFileTool(Path directory) { this.directory = directory; }
        @Tool("保存测试网页")
        public String writeFile(@P("文件名") String fileName, @P("源码") String content) throws IOException {
            if (!"index.html".equals(fileName)) throw new IllegalArgumentException("文件名不允许");
            Files.writeString(directory.resolve(fileName), content);
            return "saved " + fileName;
        }
    }
    @Test void factoryWiresPromptToolsAndRemembersMessagesAcrossRecreation() throws Exception {
        ChatModel model = mock(ChatModel.class, CALLS_REAL_METHODS);
        var registry = mock(AiModelRegistry.class);
        when(registry.getModel("code-generation")).thenReturn(model);
        var properties = new AiProperties();
        var store = new InMemoryChatMemoryStore();
        var factory = new CodeGenerationAgentFactory(registry, properties, store, new AgentPromptBuilder(), new ToolManager(properties));
        var context = new AgentContext(1, 2, CodeGenType.HTML);
        var toolRequest = ToolExecutionRequest.builder().id("tool-1").name("writeFile")
                .arguments("{\"fileName\":\"index.html\",\"content\":\"<!doctype html><html><body>Hello</body></html>\"}").build();
        doReturn(ChatResponse.builder().aiMessage(AiMessage.from(toolRequest)).build(),
                ChatResponse.builder().aiMessage(AiMessage.from("已完成")).build(),
                ChatResponse.builder().aiMessage(AiMessage.from("已记住之前的需求")).build())
                .when(model).chat(any(ChatRequest.class));
        assertEquals("已完成", factory.createAgent(context, new TestFileTool(directory)).generate("创建一个 Hello 页面"));
        assertTrue(Files.readString(directory.resolve("index.html")).contains("Hello"));
        assertEquals("已记住之前的需求", factory.createAgent(context, new TestFileTool(directory)).generate("继续修改这个页面"));
        var requests = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model, times(3)).chat(requests.capture());
        var first = requests.getAllValues().get(0);
        assertTrue(first.messages().stream().anyMatch(message -> message instanceof SystemMessage system && system.text().contains("单 HTML 文件")));
        assertTrue(requests.getAllValues().get(1).messages().stream().anyMatch(message -> message instanceof ToolExecutionResultMessage));
        assertTrue(requests.getAllValues().get(2).messages().stream().anyMatch(message -> message instanceof UserMessage user && user.singleText().contains("Hello 页面")));
        assertTrue(store.getMessages(new AgentContext(1, 3, CodeGenType.HTML).memoryId()).isEmpty());
    }
    @Test void cachedAgentUsesProductionToolsAndRebuildsWithoutLosingMemory() throws Exception {
        ChatModel model = mock(ChatModel.class, CALLS_REAL_METHODS);
        var registry = mock(AiModelRegistry.class);
        when(registry.getModel("code-generation")).thenReturn(model);
        var properties = new AiProperties(); properties.setOutputRoot(directory.toString());
        var store = new InMemoryChatMemoryStore();
        var tools = new ToolManager(properties);
        var factory = new CodeGenerationAgentFactory(registry, properties, store, new AgentPromptBuilder(), tools);
        var context = new AgentContext(1, 2, CodeGenType.HTML);
        var tool = ToolExecutionRequest.builder().id("production-1").name("writeFile")
                .arguments("{\"fileName\":\"index.html\",\"content\":\"<h1>Saved</h1>\"}").build();
        doReturn(ChatResponse.builder().aiMessage(AiMessage.from(tool)).build(),
                ChatResponse.builder().aiMessage(AiMessage.from("已保存")).build(),
                ChatResponse.builder().aiMessage(AiMessage.from("继续修改")).build()).when(model).chat(any(ChatRequest.class));
        var first = factory.getAgent(context);
        assertSame(first, factory.getAgent(context));
        assertNotSame(first, factory.getAgent(new AgentContext(2, 2, CodeGenType.HTML)));
        assertEquals("已保存", first.generate("生成网页"));
        assertEquals("<h1>Saved</h1>", Files.readString(tools.getWorkspacePath(context).resolve("index.html")));
        factory.invalidateAgent(context);
        var recreated = factory.getAgent(context);
        assertNotSame(first, recreated);
        assertFalse(store.getMessages(context.memoryId()).isEmpty());
        assertEquals("继续修改", recreated.generate("修改颜色"));
    }
    @Test void invocationsRemainSerialWhenCachedAgentIsRecreated() throws Exception {
        ChatModel model = mock(ChatModel.class, CALLS_REAL_METHODS);
        var registry = mock(AiModelRegistry.class);
        when(registry.getModel("code-generation")).thenReturn(model);
        var properties = new AiProperties(); properties.setOutputRoot(directory.toString());
        var factory = new CodeGenerationAgentFactory(registry, properties, new InMemoryChatMemoryStore(), new AgentPromptBuilder(), new ToolManager(properties));
        var context = new AgentContext(1, 2, CodeGenType.HTML);
        var first = factory.getAgent(context);
        factory.invalidateAgent(context);
        var recreated = factory.getAgent(context);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var active = new AtomicInteger();
        var calls = new AtomicInteger();
        var secondEntered = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var maximum = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() > 1) secondEntered.countDown();
            int running = active.incrementAndGet(); maximum.accumulateAndGet(running, Math::max);
            entered.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); return ChatResponse.builder().aiMessage(AiMessage.from("done")).build(); }
            finally { active.decrementAndGet(); }
        }).when(model).chat(any(ChatRequest.class));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> first.generate("first"));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var b = executor.submit(() -> { secondStarted.countDown(); return recreated.generate("second"); });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertFalse(secondEntered.await(150, TimeUnit.MILLISECONDS));
            release.countDown();
            assertEquals("done", a.get(5, TimeUnit.SECONDS));
            assertEquals("done", b.get(5, TimeUnit.SECONDS));
            assertEquals(1, maximum.get());
        } finally { release.countDown(); }
    }
    @Test void contextsAndModesAreIsolatedAndToolsAreRequired() {
        assertNotEquals(new AgentContext(1, 1, CodeGenType.HTML).memoryId(), new AgentContext(2, 1, CodeGenType.HTML).memoryId());
        assertNotEquals(new AgentContext(1, 1, CodeGenType.HTML).memoryId(), new AgentContext(1, 1, CodeGenType.MULTI_FILE).memoryId());
        assertThrows(IllegalArgumentException.class, () -> new AgentContext(0, 1, CodeGenType.HTML));
        var prompt = new AgentPromptBuilder();
        assertTrue(prompt.build(CodeGenType.MULTI_FILE).contains("style.css"));
        var factory = new CodeGenerationAgentFactory(mock(AiModelRegistry.class), new AiProperties(), new InMemoryChatMemoryStore(), prompt, new ToolManager(new AiProperties()));
        assertThrows(IllegalArgumentException.class, () -> factory.createAgent(new AgentContext(1, 1, CodeGenType.HTML)));
    }
    @Test void truncatedToolArgumentsRollbackAndSameAgentCanRetry() {
        ChatModel model = mock(ChatModel.class, CALLS_REAL_METHODS);
        var registry = mock(AiModelRegistry.class);
        when(registry.getModel("code-generation")).thenReturn(model);
        var properties = new AiProperties(); properties.setOutputRoot(directory.toString());
        var store = new InMemoryChatMemoryStore();
        var context = new AgentContext(1, 8, CodeGenType.HTML);
        var before = java.util.List.<ChatMessage>of(SystemMessage.from("旧提示词"), UserMessage.from("已成功的需求"), AiMessage.from("已成功的回复"));
        store.updateMessages(context.memoryId(), before);
        var malformed = ToolExecutionRequest.builder().id("broken").name("writeFile")
                .arguments("{\"fileName\":\"index.html\",\"content\":\"截断的源码").build();
        doReturn(ChatResponse.builder().aiMessage(AiMessage.from(malformed)).build(),
                ChatResponse.builder().aiMessage(AiMessage.from("重试成功")).build()).when(model).chat(any(ChatRequest.class));
        var factory = new CodeGenerationAgentFactory(registry, properties, store, new AgentPromptBuilder(), new ToolManager(properties));
        var agent = factory.getAgent(context);
        var failure = assertThrows(com.zzz.zerocode.exception.BusinessException.class, () -> agent.generate("失败需求"));
        assertTrue(failure.getMessage().contains("工具参数不完整"));
        assertEquals(before, store.getMessages(context.memoryId()));
        assertEquals("重试成功", agent.generate("再次尝试"));
        var requests = ArgumentCaptor.forClass(ChatRequest.class);
        verify(model, times(2)).chat(requests.capture());
        assertTrue(requests.getAllValues().get(1).messages().stream().anyMatch(message -> message instanceof UserMessage user && user.singleText().equals("已成功的需求")));
        assertFalse(requests.getAllValues().get(1).messages().stream().anyMatch(message -> message instanceof UserMessage user && user.singleText().equals("失败需求")));
    }
    @Test void legacyIncompleteToolCallsRecoverOnlyTheAffectedMemory() {
        ChatModel model = mock(ChatModel.class, CALLS_REAL_METHODS);
        var registry = mock(AiModelRegistry.class);
        when(registry.getModel("code-generation")).thenReturn(model);
        var properties = new AiProperties(); properties.setOutputRoot(directory.toString());
        var store = new InMemoryChatMemoryStore();
        var context = new AgentContext(1, 9, CodeGenType.HTML);
        var other = new AgentContext(2, 9, CodeGenType.HTML);
        var pending = ToolExecutionRequest.builder().id("unfinished").name("writeFile")
                .arguments("{\"fileName\":\"index.html\",\"content\":\"hello\"}").build();
        store.updateMessages(context.memoryId(), java.util.List.of(SystemMessage.from("system"), UserMessage.from("保留需求"),
                AiMessage.from("保留回答"), UserMessage.from("失败需求"), AiMessage.from(pending), UserMessage.from("旧重试")));
        var otherMemory = java.util.List.<ChatMessage>of(UserMessage.from("其他应用"));
        store.updateMessages(other.memoryId(), otherMemory);
        doReturn(ChatResponse.builder().aiMessage(AiMessage.from("成功")).build()).when(model).chat(any(ChatRequest.class));
        var factory = new CodeGenerationAgentFactory(registry, properties, store, new AgentPromptBuilder(), new ToolManager(properties));
        assertEquals("成功", factory.getAgent(context).generate("新重试"));
        var request = ArgumentCaptor.forClass(ChatRequest.class); verify(model).chat(request.capture());
        assertTrue(request.getValue().messages().stream().anyMatch(message -> message instanceof UserMessage user && user.singleText().equals("保留需求")));
        assertFalse(request.getValue().messages().stream().anyMatch(message -> message instanceof AiMessage ai && ai.hasToolExecutionRequests()));
        assertEquals(otherMemory, store.getMessages(other.memoryId()));
    }

}
