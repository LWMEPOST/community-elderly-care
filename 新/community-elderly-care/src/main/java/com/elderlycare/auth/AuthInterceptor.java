package com.elderlycare.auth;

import com.elderlycare.entity.User;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.exception.UnauthorizedException;
import com.elderlycare.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Objects;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    public AuthInterceptor(JwtTokenService jwtTokenService, UserService userService) {
        this.jwtTokenService = jwtTokenService;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContextHolder.clear();
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(handlerMethod)) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("缺少有效的登录凭证");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("缺少有效的登录凭证");
        }

        AuthenticatedUser tokenUser = jwtTokenService.parseToken(token);
        User user = userService.getById(tokenUser.getUserId());
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            throw new UnauthorizedException("用户不存在或已被禁用");
        }

        AuthenticatedUser currentUser = AuthenticatedUser.fromUser(user);
        UserContextHolder.setCurrentUser(currentUser);
        validateUserType(handlerMethod, currentUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private boolean isPublic(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PublicApi.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PublicApi.class);
    }

    private void validateUserType(HandlerMethod handlerMethod, AuthenticatedUser currentUser) {
        RequireUserTypes requireUserTypes = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireUserTypes.class);
        if (requireUserTypes == null) {
            requireUserTypes = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequireUserTypes.class);
        }
        if (requireUserTypes == null) {
            return;
        }
        boolean allowed = Arrays.stream(requireUserTypes.value()).anyMatch(value -> value == currentUser.getUserType());
        if (!allowed) {
            throw new ForbiddenException("当前用户无权访问该接口");
        }
    }
}
