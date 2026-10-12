package com.zzz.zerocode.ai.prompt;

import com.zzz.zerocode.ai.model.CodeGenType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AgentPromptBuilder {
    private final String common = load("prompts/codegen-common.txt");
    private final String html = load("prompts/codegen-html.txt");
    private final String multiFile = load("prompts/codegen-multi-file.txt");

    public String build(CodeGenType mode) {
        if (mode == null) throw new IllegalArgumentException("生成模式不能为空");
        return common + "\n\n" + switch (mode) { case HTML -> html; case MULTI_FILE -> multiFile; };
    }
    private String load(String path) {
        try { return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8).strip(); }
        catch (IOException e) { throw new IllegalStateException("无法加载系统提示词：" + path, e); }
    }
}
