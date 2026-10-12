package com.zzz.zerocode.model.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegisterRequest {
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "账号须为 4–32 位字母、数字或下划线")
    private String userAccount;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度须为 8–64 位")
    private String userPassword;
    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 64, message = "确认密码长度须为 8–64 位")
    private String checkPassword;
}
