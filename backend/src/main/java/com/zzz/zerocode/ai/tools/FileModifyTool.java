package com.zzz.zerocode.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileModifyTool {
    private final AppWorkspace workspace;
    @Tool("精确替换当前应用文件中唯一匹配的原文本。原文本匹配多处或不存在时不会修改文件。")
    public String modifyFile(@P("当前应用文件名") String fileName,
                             @P("需要替换的原文本，须包含足够的唯一上下文") String oldText,
                             @P("替换后的新文本，可为空字符串以删除原文本") String newText) {
        return workspace.modify(fileName, oldText, newText);
    }
}
