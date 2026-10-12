package com.zzz.zerocode.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("userAccount")
    private String userAccount;
    @JsonIgnore
    @TableField("userPassword")
    private String userPassword;
    @TableField("userName")
    private String userName;
    @TableField("userAvatar")
    private String userAvatar;
    @TableField("userProfile")
    private String userProfile;
    @TableField("userRole")
    private String userRole;
    @TableField(value = "editTime", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime editTime;
    @TableField(value = "createTime", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
    @TableField(value = "updateTime", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")
    @TableField("isDelete")
    private Integer isDelete;
}
