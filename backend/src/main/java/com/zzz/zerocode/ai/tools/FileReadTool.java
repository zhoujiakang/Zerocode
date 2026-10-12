package com.zzz.zerocode.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileReadTool {
    private final AppWorkspace workspace;
    @Tool("读取当前应用文件的完整源码，修改前先读取。")
    public String readFile(@P("当前应用文件名") String fileName) { return workspace.read(fileName); }
}
