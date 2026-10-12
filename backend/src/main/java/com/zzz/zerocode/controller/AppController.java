package com.zzz.zerocode.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.zzz.zerocode.common.*;
import com.zzz.zerocode.model.dto.app.*;
import com.zzz.zerocode.model.vo.*;
import com.zzz.zerocode.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@SaCheckLogin
@Tag(name = "AI 应用模块")
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {
    private final AppService appService;
    @Operation(summary = "创建应用（模式创建后不可修改）")
    @PostMapping("/add")
    public BaseResponse<String> create(@Valid @RequestBody AppCreateRequest request) { return ResultUtils.success(appService.create(request)); }
    @Operation(summary = "分页查询我的应用")
    @PostMapping("/list/my")
    public BaseResponse<PageResponse<AppVO>> list(@Valid @RequestBody PageRequest request) { return ResultUtils.success(appService.list(request)); }
    @Operation(summary = "查询我的应用详情")
    @GetMapping("/{id}")
    public BaseResponse<AppVO> get(@PathVariable long id) { return ResultUtils.success(appService.get(id)); }
    @Operation(summary = "生成或继续修改应用（同步执行）")
    @PostMapping("/generate")
    public BaseResponse<AppVO> generate(@Valid @RequestBody AppGenerateRequest request) { return ResultUtils.success(appService.generate(request)); }
    @Operation(summary = "按时间顺序分页获取完整用户对话与生成结果历史")
    @PostMapping("/{id}/history")
    public BaseResponse<PageResponse<ChatHistoryVO>> history(@PathVariable long id, @Valid @RequestBody PageRequest request) {
        return ResultUtils.success(appService.history(id, request));
    }
    @Operation(summary = "查询完整生成文件列表")
    @GetMapping("/{id}/files")
    public BaseResponse<List<String>> files(@PathVariable long id) { return ResultUtils.success(appService.files(id)); }
    @Operation(summary = "读取生成源码（JSON 文本，不直接执行 HTML）")
    @GetMapping("/{id}/file")
    public BaseResponse<String> file(@PathVariable long id, @RequestParam String fileName) { return ResultUtils.success(appService.readFile(id, fileName)); }
}
