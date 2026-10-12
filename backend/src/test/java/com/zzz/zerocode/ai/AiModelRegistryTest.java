package com.zzz.zerocode.ai;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.exception.BusinessException;
import com.zzz.zerocode.exception.ErrorCode;
import org.junit.jupiter.api.Test;
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
}
