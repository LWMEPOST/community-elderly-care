package com.elderlycare.controller;

import com.elderlycare.auth.AuthenticatedUser;
import com.elderlycare.auth.JwtTokenService;
import com.elderlycare.auth.PublicApi;
import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.auth.UserContextHolder;
import com.elderlycare.common.Result;
import com.elderlycare.dto.ChangePasswordRequest;
import com.elderlycare.dto.FamilyBindingRequest;
import com.elderlycare.dto.LoginRequest;
import com.elderlycare.dto.LoginResponse;
import com.elderlycare.dto.RegisterRequest;
import com.elderlycare.dto.UpdateUserRequest;
import com.elderlycare.dto.UserView;
import com.elderlycare.entity.FamilyBinding;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @PublicApi
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user != null) {
            String token = jwtTokenService.generateToken(user);
            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expireAt(jwtTokenService.getExpireAt(token))
                    .user(UserView.from(user))
                    .build();
            return Result.success("登录成功", response);
        }
        User existedUser = userService.getByUsername(request.getUsername());
        if (existedUser != null && !Integer.valueOf(1).equals(existedUser.getStatus())) {
            return Result.error(403, "账号待审核或已禁用，请联系管理员");
        }
        return Result.error(401, "用户名或密码错误");
    }

    @PublicApi
    @PostMapping("/register")
    public Result<UserView> register(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = userService.register(request);
        return Result.success("注册申请已提交，请等待管理员审核", UserView.from(registeredUser));
    }

    @GetMapping("/me")
    public Result<UserView> getCurrentUser() {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        User user = userService.getUserInfo(currentUser.getUserId());
        return Result.success(UserView.from(user));
    }

    @GetMapping("/{id}")
    public Result<UserView> getUserInfo(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        if (!currentUser.isAdmin() && !currentUser.getUserId().equals(id)) {
            throw new ForbiddenException("只能查看自己的用户信息");
        }
        User user = userService.getUserInfo(id);
        if (user != null) {
            return Result.success(UserView.from(user));
        }
        return Result.error(404, "用户不存在");
    }

    @PutMapping("/update")
    public Result<UserView> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        Long targetUserId = currentUser.getUserId();
        if (request.getId() != null) {
            if (!currentUser.isAdmin() && !currentUser.getUserId().equals(request.getId())) {
                throw new ForbiddenException("只能更新自己的用户信息");
            }
            targetUserId = request.getId();
        }
        User user = userService.updateUserInfo(targetUserId, request);
        return Result.success("更新成功", UserView.from(user));
    }

    @PutMapping("/password")
    public Result<Boolean> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        userService.changePassword(currentUser.getUserId(), request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功", true);
    }

    @RequireUserTypes({2, 3})
    @PostMapping("/binding")
    public Result<FamilyBinding> bindingFamily(@Valid @RequestBody FamilyBindingRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        boolean autoConfirm = currentUser.isAdmin();
        Long familyId = autoConfirm ? request.getFamilyId() : currentUser.getUserId();
        if (familyId == null) {
            throw new BusinessException(400, "familyId不能为空");
        }
        FamilyBinding binding = userService.bindingFamily(
                request.getElderlyId(), familyId, request.getRelation(), autoConfirm);
        String message = Integer.valueOf(1).equals(binding.getStatus())
                ? "绑定成功"
                : "绑定申请已提交，请等待老人或管理员确认";
        return Result.success(message, binding);
    }
}
