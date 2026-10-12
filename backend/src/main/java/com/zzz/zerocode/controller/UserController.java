package com.zzz.zerocode.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.zzz.zerocode.common.*;
import com.zzz.zerocode.model.dto.user.*;
import com.zzz.zerocode.model.vo.UserVO;
import com.zzz.zerocode.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @Operation(summary = "用户注册（注册角色固定为普通用户）")
    @PostMapping("/register")
    public BaseResponse<String> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResultUtils.success(Long.toString(userService.register(request)));
    }
    @Operation(summary = "用户登录（通过 HttpOnly Cookie 保持会话）")
    @PostMapping("/login")
    public BaseResponse<UserVO> login(@Valid @RequestBody UserLoginRequest request) {
        return ResultUtils.success(userService.login(request));
    }
    @SaCheckLogin
    @Operation(summary = "获取当前登录用户（脱敏信息）")
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser() { return ResultUtils.success(userService.getLoginUser()); }
    @Operation(summary = "用户退出登录")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout() { return ResultUtils.success(userService.logout()); }
    @SaCheckRole("admin")
    @Operation(summary = "分页搜索用户（仅管理员）")
    @PostMapping("/list/page")
    public BaseResponse<PageResponse<UserVO>> search(@Valid @RequestBody UserQueryRequest request) {
        return ResultUtils.success(userService.search(request));
    }
    @SaCheckRole("admin")
    @Operation(summary = "逻辑删除用户并注销其会话（仅管理员）")
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@Valid @RequestBody DeleteRequest request) {
        return ResultUtils.success(userService.delete(request.getId()));
    }
}
