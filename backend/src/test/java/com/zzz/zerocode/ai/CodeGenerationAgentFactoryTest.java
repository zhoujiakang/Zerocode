package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.ai.model.*;
import com.zzz.zerocode.ai.prompt.AgentPromptBuilder;
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
        var factory = new CodeGenerationAgentFactory(registry, properties, store, new AgentPromptBuilder());
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
    @Test void contextsAndModesAreIsolatedAndToolsAreRequired() {
        assertNotEquals(new AgentContext(1, 1, CodeGenType.HTML).memoryId(), new AgentContext(2, 1, CodeGenType.HTML).memoryId());
        assertNotEquals(new AgentContext(1, 1, CodeGenType.HTML).memoryId(), new AgentContext(1, 1, CodeGenType.MULTI_FILE).memoryId());
        assertThrows(IllegalArgumentException.class, () -> new AgentContext(0, 1, CodeGenType.HTML));
        var prompt = new AgentPromptBuilder();
        assertTrue(prompt.build(CodeGenType.MULTI_FILE).contains("style.css"));
        var factory = new CodeGenerationAgentFactory(mock(AiModelRegistry.class), new AiProperties(), new InMemoryChatMemoryStore(), prompt);
        assertThrows(IllegalArgumentException.class, () -> factory.createAgent(new AgentContext(1, 1, CodeGenType.HTML)));
    }
}
