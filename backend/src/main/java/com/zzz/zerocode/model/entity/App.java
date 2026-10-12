package com.zzz.zerocode.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("`app`")
public class App {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("appName")
    private String appName;
    @TableField("initPrompt")
    private String initPrompt;
    @TableField("codeGenType")
    private String codeGenType;
    @TableField("userId")
    private Long userId;
    @TableField("status")
    private String status;
    @TableField("lastError")
    private String lastError;
    @TableField(value = "createTime", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
    @TableField(value = "updateTime", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
