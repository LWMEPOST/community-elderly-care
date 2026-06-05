package com.elderlycare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elderlycare.dto.ElderlyProfileRequest;
import com.elderlycare.dto.RegisterRequest;
import com.elderlycare.dto.UpdateUserRequest;
import com.elderlycare.entity.ElderlyInfo;
import com.elderlycare.entity.FamilyBinding;
import com.elderlycare.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    User login(String username, String password);
    User getByUsername(String username);
    User register(RegisterRequest request);
    User getUserInfo(Long userId);
    User updateUserInfo(Long userId, UpdateUserRequest request);
    FamilyBinding bindingFamily(Long elderlyId, Long familyId, String relation, boolean autoConfirm);
    void changePassword(Long userId, String oldPassword, String newPassword);
    List<User> listUsers(String keyword, Integer userType, Integer status);
    User updateUserStatus(Long operatorUserId, Long targetUserId, Integer status);
    List<FamilyBinding> listBindings(Long operatorUserId, Integer operatorUserType,
                                     Long elderlyId, Long familyId, Integer status);
    FamilyBinding confirmBinding(Long bindingId, Long operatorUserId, Integer operatorUserType);
    boolean hasConfirmedBinding(Long elderlyId, Long familyId);
    List<Long> listConfirmedFamilyIds(Long elderlyId);
    List<Long> listConfirmedElderlyIds(Long familyId);
    ElderlyInfo getElderlyInfo(Long targetUserId, Long operatorUserId, Integer operatorUserType);
    ElderlyInfo saveElderlyInfo(Long targetUserId, ElderlyProfileRequest request);
}
