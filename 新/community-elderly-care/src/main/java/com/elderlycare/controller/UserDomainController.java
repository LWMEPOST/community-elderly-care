package com.elderlycare.controller;

import com.elderlycare.auth.AuthenticatedUser;
import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.auth.UserContextHolder;
import com.elderlycare.common.Result;
import com.elderlycare.dto.ElderlyProfileRequest;
import com.elderlycare.dto.ElderlyProfileView;
import com.elderlycare.dto.FamilyBindingView;
import com.elderlycare.dto.UserStatusUpdateRequest;
import com.elderlycare.dto.UserView;
import com.elderlycare.entity.ElderlyInfo;
import com.elderlycare.entity.FamilyBinding;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/api/user")
public class UserDomainController {

    @Autowired
    private UserService userService;

    @RequireUserTypes({1, 2, 3})
    @GetMapping("/elderly-profile")
    public Result<ElderlyProfileView> getElderlyProfile(@RequestParam(required = false) Long userId) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        Long targetUserId = resolveTargetElderlyUserId(userId, currentUser);
        ElderlyInfo elderlyInfo = userService.getElderlyInfo(targetUserId, currentUser.getUserId(), currentUser.getUserType());
        User targetUser = userService.getUserInfo(targetUserId);
        return Result.success(ElderlyProfileView.from(targetUser, elderlyInfo));
    }

    @RequireUserTypes({1, 3})
    @PutMapping("/elderly-profile")
    public Result<ElderlyProfileView> saveElderlyProfile(@Valid @RequestBody ElderlyProfileRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        Long targetUserId = resolveTargetElderlyUserId(request.getUserId(), currentUser);
        if (!currentUser.isAdmin() && !Objects.equals(currentUser.getUserId(), targetUserId)) {
            throw new ForbiddenException("只能维护自己的老人资料");
        }
        ElderlyInfo elderlyInfo = userService.saveElderlyInfo(targetUserId, request);
        User targetUser = userService.getUserInfo(targetUserId);
        return Result.success("老人资料保存成功", ElderlyProfileView.from(targetUser, elderlyInfo));
    }

    @RequireUserTypes({1, 2, 3})
    @GetMapping("/bindings")
    public Result<List<FamilyBindingView>> listBindings(@RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) Long elderlyId,
                                                        @RequestParam(required = false) Long familyId) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        List<FamilyBinding> bindings = userService.listBindings(
                currentUser.getUserId(), currentUser.getUserType(), elderlyId, familyId, status);
        return Result.success(toBindingViews(bindings));
    }

    @RequireUserTypes({1, 3})
    @PutMapping("/binding/{id}/confirm")
    public Result<FamilyBindingView> confirmBinding(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        FamilyBinding binding = userService.confirmBinding(id, currentUser.getUserId(), currentUser.getUserType());
        return Result.success("绑定确认成功", toBindingViews(List.of(binding)).get(0));
    }

    @RequireUserTypes({3})
    @GetMapping("/admin/users")
    public Result<List<UserView>> listUsers(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer userType,
                                            @RequestParam(required = false) Integer status) {
        List<UserView> users = userService.listUsers(keyword, userType, status)
                .stream()
                .map(UserView::from)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @RequireUserTypes({3})
    @PutMapping("/admin/status")
    public Result<UserView> updateUserStatus(@Valid @RequestBody UserStatusUpdateRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        User user = userService.updateUserStatus(currentUser.getUserId(), request.getUserId(), request.getStatus());
        return Result.success("用户状态更新成功", UserView.from(user));
    }

    private Long resolveTargetElderlyUserId(Long userId, AuthenticatedUser currentUser) {
        if (userId != null) {
            return userId;
        }
        if (Objects.equals(currentUser.getUserType(), 1)) {
            return currentUser.getUserId();
        }
        throw new BusinessException(400, "请指定老人用户ID");
    }

    private List<FamilyBindingView> toBindingViews(List<FamilyBinding> bindings) {
        if (bindings.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        bindings.forEach(binding -> {
            userIds.add(binding.getElderlyId());
            userIds.add(binding.getFamilyId());
        });
        Map<Long, User> userMap = userService.listByIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return bindings.stream()
                .map(binding -> FamilyBindingView.from(
                        binding, userMap.get(binding.getElderlyId()), userMap.get(binding.getFamilyId())))
                .collect(Collectors.toList());
    }
}
