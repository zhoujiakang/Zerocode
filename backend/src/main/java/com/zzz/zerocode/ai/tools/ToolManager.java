package com.zzz.zerocode.ai.tools;

import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.ai.model.AgentContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

/** 工具对象按应用创建，共享该应用的工作目录；不持有全局可变的当前用户/应用。 */
@Component
@RequiredArgsConstructor
public class ToolManager {
    private final AiProperties properties;
    public Object[] getTools(AgentContext context) {
        AppWorkspace workspace = new AppWorkspace(Path.of(properties.getOutputRoot()), context, properties.getMaxFileBytes());
        return new Object[]{new FileWriteTool(workspace), new FileReadTool(workspace),
                new FileModifyTool(workspace), new FileDirReadTool(workspace)};
    }
    public Path getWorkspacePath(AgentContext context) {
        return new AppWorkspace(Path.of(properties.getOutputRoot()), context, properties.getMaxFileBytes()).directory();
    }
}
