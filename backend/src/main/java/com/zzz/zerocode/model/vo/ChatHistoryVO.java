package com.zzz.zerocode.model.vo;

import com.zzz.zerocode.model.entity.ChatHistory;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record ChatHistoryVO(@JsonSerialize(using = ToStringSerializer.class) Long id,
        String role,
        String content,
        LocalDateTime createTime) {
    public static ChatHistoryVO from(ChatHistory value) { return new ChatHistoryVO(value.getId(), value.getRole(), value.getContent(), value.getCreateTime()); }
}
