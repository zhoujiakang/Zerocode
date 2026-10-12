package com.zzz.zerocode.model.vo;

import com.zzz.zerocode.model.entity.User;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

/** 对外视图显式排除密码，Long id 转为字符串避免 JavaScript 精度丢失。 */
public record UserVO(@JsonSerialize(using = ToStringSerializer.class) Long id,
                     String userAccount, String userName, String userAvatar,
                     String userProfile, String userRole, LocalDateTime createTime) {
    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUserAccount(), user.getUserName(),
                user.getUserAvatar(), user.getUserProfile(), user.getUserRole(), user.getCreateTime());
    }
}
