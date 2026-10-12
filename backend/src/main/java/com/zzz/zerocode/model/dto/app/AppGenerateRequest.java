package com.zzz.zerocode.model.dto.app;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AppGenerateRequest {
    @NotNull @Positive
    private Long appId;
    /** 首次调用可省略，使用创建应用时的描述；后续修改必须提供新描述。 */
    @Size(max = 10000)
    private String message;
}
