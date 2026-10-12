package com.zzz.zerocode.ai;

import dev.langchain4j.service.UserMessage;

/** 源码通过工具落盘，最终文本仅用于说明结果。 */
public interface CodeGenerationAgent {
    String generate(@UserMessage String description);
}
