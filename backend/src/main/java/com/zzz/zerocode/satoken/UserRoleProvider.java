package com.zzz.zerocode.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.zzz.zerocode.mapper.UserMapper;
import com.zzz.zerocode.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRoleProvider implements StpInterface {
    private final UserMapper userMapper;
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 每次鉴权查询有效用户，删除或修改角色后立即生效。
        User user = userMapper.selectById(Long.parseLong(loginId.toString()));
        return user == null ? List.of() : List.of(user.getUserRole());
    }
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return getRoleList(loginId, loginType).contains("admin") ? List.of("*") : List.of();
    }
}
