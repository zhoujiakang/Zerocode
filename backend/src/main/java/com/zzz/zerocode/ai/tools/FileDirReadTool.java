package com.zzz.zerocode.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileDirReadTool {
    private final AppWorkspace workspace;
    @Tool("列出当前应用已保存的网页文件与大小，用于核对文件是否齐全。")
    public String listFiles() { return workspace.list(); }
}
