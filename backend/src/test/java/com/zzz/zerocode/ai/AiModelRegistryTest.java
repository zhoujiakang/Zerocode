package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.exception.BusinessException;
import com.zzz.zerocode.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AiModelRegistryTest {
    private AiProperties.ModelConfig config(String modelName) {
        var config = new AiProperties.ModelConfig();
        config.setBaseUrl("https://example.invalid/v1");
        config.setModelName(modelName);
        config.setApiKey("unit-test-key");
        return config;
    }
    @Test void disabledOrMissingConfigurationReturnsExplicitError() {
        var properties = new AiProperties();
        var registry = new AiModelRegistry(properties);
        assertEquals(ErrorCode.AI_NOT_CONFIGURED, assertThrows(BusinessException.class, () -> registry.getModel("code-generation")).getErrorCode());
        properties.setEnabled(true);
        assertThrows(BusinessException.class, () -> registry.getModel("missing"));
        properties.getModels().put("empty", new AiProperties.ModelConfig());
        assertThrows(BusinessException.class, () -> registry.getModel("empty"));
    }
    @Test void namedModelsAreIndependentAndReusedWithoutMakingRequests() {
        var properties = new AiProperties(); properties.setEnabled(true);
        properties.getModels().put("code-generation", config("coding-model"));
        properties.getModels().put("planning", config("planning-model"));
        var registry = new AiModelRegistry(properties);
        assertSame(registry.getModel("code-generation"), registry.getModel("code-generation"));
        assertNotSame(registry.getModel("code-generation"), registry.getModel("planning"));
        assertFalse(properties.toString().contains("unit-test-key"));
    }
    @Test void vendorParametersAreSentInChatCompletionBody() throws Exception {
        var json = new ObjectMapper();
        var captured = new AtomicReference<com.fasterxml.jackson.databind.JsonNode>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            captured.set(json.readTree(exchange.getRequestBody()));
            byte[] response = "{\"id\":\"test\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"test-model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        try {
            var properties = new AiProperties(); properties.setEnabled(true);
            var config = config("test-model");
            config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            config.setCustomParameters(Map.of("thinking", Map.of("type", "disabled")));
            properties.getModels().put("deepseek", config);
            assertEquals("ok", new AiModelRegistry(properties).getModel("deepseek").chat("hello"));
            assertEquals("disabled", captured.get().path("thinking").path("type").asText());
            assertEquals("test-model", captured.get().path("model").asText());
            assertFalse(properties.toString().contains("unit-test-key"));
        } finally { server.stop(0); }
    }

}
