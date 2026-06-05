package com.elderlycare.auth;

import com.elderlycare.exception.UnauthorizedException;

public final class UserContextHolder {

    private static final ThreadLocal<AuthenticatedUser> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setCurrentUser(AuthenticatedUser user) {
        HOLDER.set(user);
    }

    public static AuthenticatedUser getCurrentUser() {
        return HOLDER.get();
    }

    public static AuthenticatedUser requireCurrentUser() {
        AuthenticatedUser user = HOLDER.get();
        if (user == null) {
            throw new UnauthorizedException("请先登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
