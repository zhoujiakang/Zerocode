package com.zzz.zerocode.ai.model;

/** 由业务层在校验归属后构造；记忆范围与应用、用户、模式保持一致。 */
public record AgentContext(long userId, long appId, CodeGenType mode) {
    public AgentContext {
        if (userId <= 0 || appId <= 0 || mode == null) throw new IllegalArgumentException("用户、应用和生成模式不能为空或无效");
    }
    public String memoryId() { return userId + ":" + appId + ":" + mode.name(); }
}
