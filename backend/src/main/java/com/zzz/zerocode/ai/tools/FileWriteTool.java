package com.zzz.zerocode.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileWriteTool {
    private final AppWorkspace workspace;
    @Tool("将完整源码写入当前应用文件，文件存在时覆盖。只允许当前模式的固定文件名。")
    public String writeFile(@P("文件名，例如 index.html") String fileName, @P("完整源码，不含 Markdown 围栏") String content) {
        workspace.write(fileName, content);
        return "已保存 " + fileName;
    }
}
