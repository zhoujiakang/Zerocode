package com.zzz.zerocode.service;

import cn.dev33.satoken.stp.StpUtil;
import com.zzz.zerocode.common.PageResponse;
import com.zzz.zerocode.exception.*;
import com.zzz.zerocode.model.dto.user.*;
import com.zzz.zerocode.model.entity.User;
import com.zzz.zerocode.model.vo.UserVO;
import com.zzz.zerocode.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    public long register(UserRegisterRequest request) {
        validatePasswordBytes(request.getUserPassword());
        ThrowUtils.throwIf(!request.getUserPassword().equals(request.getCheckPassword()), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        try {
            // 数据库唯一索引负责并发注册冲突，注册无法指定角色。
            User user = new User();
            user.setUserAccount(normalizeAccount(request.getUserAccount()));
            user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
            user.setUserName(user.getUserAccount());
            user.setUserRole("user");
            user.setIsDelete(0);
            ThrowUtils.throwIf(userMapper.insert(user) != 1 || user.getId() == null, ErrorCode.OPERATION_ERROR);
            return user.getId();
        } catch (DuplicateKeyException e) { throw new BusinessException(ErrorCode.ACCOUNT_EXISTS); }
    }
    public UserVO login(UserLoginRequest request) {
        validatePasswordBytes(request.getUserPassword());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, normalizeAccount(request.getUserAccount())));
        ThrowUtils.throwIf(user == null, ErrorCode.LOGIN_FAILED);
        ThrowUtils.throwIf(!passwordEncoder.matches(request.getUserPassword(), user.getUserPassword()), ErrorCode.LOGIN_FAILED);
        // 成功登录后重新签发会话，旧浏览器令牌失效；失败登录不影响已有会话。
        if (StpUtil.isLogin()) StpUtil.logout();
        StpUtil.login(user.getId());
        return UserVO.from(user);
    }
    public UserVO getLoginUser() {
        long id = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(id);
        if (user == null) { StpUtil.logout(); throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR); }
        return UserVO.from(user);
    }
    public boolean logout() { StpUtil.logout(); return true; }
    public PageResponse<UserVO> search(UserQueryRequest request) {
        ThrowUtils.throwIf(request.getCurrent() > 1_000_000, ErrorCode.PARAMS_ERROR, "页码过大");
        var query = new LambdaQueryWrapper<User>();
        query.eq(request.getId() != null, User::getId, request.getId());
        if (request.getUserAccount() != null && !request.getUserAccount().isBlank()) {
            query.like(User::getUserAccount, escapeLikeKeyword(request.getUserAccount().trim()));
        }
        if (request.getUserName() != null && !request.getUserName().isBlank()) {
            query.like(User::getUserName, escapeLikeKeyword(request.getUserName().trim()));
        }
        query.eq(request.getUserRole() != null, User::getUserRole, request.getUserRole());
        var page = userMapper.selectPage(new Page<User>(request.getCurrent(), request.getPageSize()), query);
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(UserVO::from).toList());
    }
    /** 保持按字面包含匹配，避免输入 % 或 _ 时扩大查询范围。 */
    private String escapeLikeKeyword(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
    public boolean delete(long id) {
        long currentId = StpUtil.getLoginIdAsLong();
        ThrowUtils.throwIf(id == currentId, ErrorCode.PARAMS_ERROR, "不能删除当前登录的管理员");
        ThrowUtils.throwIf(userMapper.deleteById(id) != 1, ErrorCode.NOT_FOUND_ERROR, "用户不存在或已删除");
        // 使该用户所有设备的登录会话立即失效，后续也会检查 isDelete。
        StpUtil.kickout(id);
        return true;
    }
    private void validatePasswordBytes(String password) {
        ThrowUtils.throwIf(password.getBytes(StandardCharsets.UTF_8).length > 72, ErrorCode.PARAMS_ERROR, "密码 UTF-8 长度不能超过 72 字节");
    }
    private String normalizeAccount(String account) { return account.toLowerCase(Locale.ROOT); }
}
