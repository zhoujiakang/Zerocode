package com.zzz.zerocode.model.dto.app;

import com.zzz.zerocode.ai.model.CodeGenType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AppCreateRequest {
    @NotBlank @Size(max = 128)
    private String appName;
    @NotBlank @Size(max = 10000)
    private String initPrompt;
    @NotNull
    private CodeGenType codeGenType;
}
