package com.zzz.zerocode.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("`chat_history`")
public class ChatHistory {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("appId")
    private Long appId;
    @TableField("userId")
    private Long userId;
    @TableField("role")
    private String role;
    @TableField("content")
    private String content;
    @TableField(value = "createTime", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
