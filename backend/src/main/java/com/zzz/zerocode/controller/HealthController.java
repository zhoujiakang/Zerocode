package com.zzz.zerocode.controller;

import com.zzz.zerocode.common.BaseResponse;
import com.zzz.zerocode.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Tag(name = "服务状态")
@RestController
public class HealthController {
    @Operation(summary = "检查 HTTP 服务状态（不检测数据库连接）")
    @GetMapping("/health")
    public BaseResponse<Map<String, String>> health() {
        return ResultUtils.success(Map.of("status", "UP", "application", "ZeroCode"));
    }
}
