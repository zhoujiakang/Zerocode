package com.zzz.zerocode.model.vo;

import com.zzz.zerocode.model.entity.App;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record AppVO(@JsonSerialize(using = ToStringSerializer.class) Long id,
        String appName,
        String initPrompt,
        String codeGenType,
        String status,
        String lastError,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
    public static AppVO from(App value) { return new AppVO(value.getId(), value.getAppName(), value.getInitPrompt(), value.getCodeGenType(), value.getStatus(), value.getLastError(), value.getCreateTime(), value.getUpdateTime()); }
}
