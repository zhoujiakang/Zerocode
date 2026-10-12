package com.zzz.zerocode.model.dto.user;

import com.zzz.zerocode.common.PageRequest;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest {
    @Positive(message = "用户 id 必须大于 0")
    private Long id;
    @Size(max = 256, message = "账号查询条件过长")
    private String userAccount;
    @Size(max = 256, message = "昵称查询条件过长")
    private String userName;
    @Pattern(regexp = "^(user|admin)$", message = "角色仅支持 user 或 admin")
    private String userRole;
}
