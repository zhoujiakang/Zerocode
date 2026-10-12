package com.zzz.zerocode.ai.tools;

import com.zzz.zerocode.ai.model.*;
import com.zzz.zerocode.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Pattern;

/** 检查必需文件和基本文档结构；不执行生成的 JavaScript，也不作为源码安全审计。 */
@Component
@RequiredArgsConstructor
public class GeneratedFilesValidator {
    private final ToolManager toolManager;

    public List<String> validate(AgentContext context) {
        AppWorkspace workspace = toolManager.getWorkspace(context);
        List<String> files = context.mode() == CodeGenType.HTML ? List.of("index.html")
                : List.of("index.html", "style.css", "script.js");
        try {
            for (String file : files) require(!workspace.read(file).isBlank(), "生成文件内容为空：" + file);
            String html = workspace.read("index.html");
            require(matches(html, "<!doctype\\s+html\\s*>") && matches(html, "<head(?:\\s[^>]*)?>")
                    && matches(html, "</head\\s*>") && matches(html, "<body(?:\\s[^>]*)?>")
                    && matches(html, "</body\\s*>"), "index.html 缺少完整 HTML 文档结构");
            if (context.mode() == CodeGenType.MULTI_FILE) {
                require(matches(html, "<link\\b[^>]*href\\s*=\\s*['\"](?:\\./)?style\\.css['\"][^>]*>"), "index.html 未引用 style.css");
                require(matches(html, "<script\\b[^>]*src\\s*=\\s*['\"](?:\\./)?script\\.js['\"][^>]*>"), "index.html 未引用 script.js");
            }
            return files;
        } catch (BusinessException e) { throw e; }
        catch (RuntimeException e) { throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成文件不完整或不可读取，请重试"); }
    }
    private boolean matches(String content, String regex) { return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(content).find(); }
    private void require(boolean condition, String message) { ThrowUtils.throwIf(!condition, ErrorCode.OPERATION_ERROR, message); }
}
