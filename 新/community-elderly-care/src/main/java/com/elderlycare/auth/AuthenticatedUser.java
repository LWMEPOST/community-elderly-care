package com.elderlycare.auth;

import com.elderlycare.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticatedUser {

    private Long userId;
    private String username;
    private String realName;
    private Integer userType;
    private Integer status;

    public boolean isAdmin() {
        return Integer.valueOf(3).equals(userType);
    }

    public static AuthenticatedUser fromUser(User user) {
        return AuthenticatedUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .status(user.getStatus())
                .build();
    }
}
