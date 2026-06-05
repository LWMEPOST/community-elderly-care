package com.elderlycare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elderlycare.dto.ElderlyProfileRequest;
import com.elderlycare.dto.RegisterRequest;
import com.elderlycare.dto.UpdateUserRequest;
import com.elderlycare.entity.ElderlyInfo;
import com.elderlycare.entity.FamilyBinding;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.mapper.ElderlyInfoMapper;
import com.elderlycare.mapper.FamilyBindingMapper;
import com.elderlycare.mapper.UserMapper;
import com.elderlycare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private ElderlyInfoMapper elderlyInfoMapper;

    @Autowired
    private FamilyBindingMapper familyBindingMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User login(String username, String password) {
        User user = getByUsername(username);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            return null;
        }
        if (!matchesPassword(password, user.getPassword())) {
            return null;
        }
        upgradeLegacyPlaintextPassword(user, password);
        return user;
    }

    @Override
    public User getByUsername(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername == null) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, normalizedUsername)
                .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }

    @Override
    public User register(RegisterRequest request) {
        String normalizedUsername = normalizeUsername(request.getUsername());
        String normalizedRealName = normalizeRequiredText(request.getRealName());
        String normalizedPhone = normalizePhone(request.getPhone());
        String normalizedAddress = normalizeOptionalText(request.getAddress());
        String normalizedEmergencyContact = normalizeOptionalText(request.getEmergencyContact());
        String normalizedEmergencyPhone = normalizeOptionalPhone(request.getEmergencyPhone());
        if (normalizedUsername == null || normalizedRealName == null || normalizedPhone == null) {
            throw new BusinessException(400, "注册信息不完整");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, normalizedUsername);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(409, "用户名已存在");
        }
        validateRegisterUserType(request.getUserType());

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(normalizedRealName);
        user.setPhone(normalizedPhone);
        user.setUserType(request.getUserType());
        user.setAddress(normalizedAddress);
        user.setEmergencyContact(normalizedEmergencyContact);
        user.setEmergencyPhone(normalizedEmergencyPhone);
        user.setStatus(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        this.save(user);
        return user;
    }

    @Override
    public User getUserInfo(Long userId) {
        return this.getById(userId);
    }

    @Override
    public User updateUserInfo(Long userId, UpdateUserRequest request) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getEmergencyContact() != null) {
            user.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getEmergencyPhone() != null) {
            user.setEmergencyPhone(request.getEmergencyPhone());
        }
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        return user;
    }

    @Override
    @Transactional
    public FamilyBinding bindingFamily(Long elderlyId, Long familyId, String relation, boolean autoConfirm) {
        User elderlyUser = getRequiredUser(elderlyId, "老人用户不存在");
        User familyUser = getRequiredUser(familyId, "家属用户不存在");
        validateUserType(elderlyUser, 1, "老人用户不存在或类型不正确");
        validateUserType(familyUser, 2, "家属用户不存在或类型不正确");
        validateUserEnabled(elderlyUser, "老人用户未启用，暂不能发起绑定");
        validateUserEnabled(familyUser, "家属用户未启用，暂不能发起绑定");

        int targetStatus = autoConfirm ? 1 : 0;
        LambdaQueryWrapper<FamilyBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyBinding::getElderlyId, elderlyId)
                .eq(FamilyBinding::getFamilyId, familyId)
                .last("LIMIT 1");
        FamilyBinding existingBinding = familyBindingMapper.selectOne(wrapper);
        if (existingBinding != null) {
            existingBinding.setRelation(relation);
            if (autoConfirm) {
                existingBinding.setStatus(1);
            } else if (!Objects.equals(existingBinding.getStatus(), 1)) {
                existingBinding.setStatus(targetStatus);
            }
            familyBindingMapper.updateById(existingBinding);
            return existingBinding;
        }

        FamilyBinding binding = new FamilyBinding();
        binding.setElderlyId(elderlyId);
        binding.setFamilyId(familyId);
        binding.setRelation(relation);
        binding.setStatus(targetStatus);
        binding.setCreateTime(LocalDateTime.now());
        familyBindingMapper.insert(binding);
        return binding;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getRequiredUser(userId, "用户不存在");
        if (!matchesPassword(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
    }

    @Override
    public List<User> listUsers(String keyword, Integer userType, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (userType != null) {
            wrapper.eq(User::getUserType, userType);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(condition -> condition
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public User updateUserStatus(Long operatorUserId, Long targetUserId, Integer status) {
        if (!Objects.equals(status, 0) && !Objects.equals(status, 1)) {
            throw new BusinessException(400, "状态值不正确");
        }
        if (Objects.equals(operatorUserId, targetUserId) && Objects.equals(status, 0)) {
            throw new BusinessException(400, "不能禁用当前登录管理员账号");
        }
        User user = getRequiredUser(targetUserId, "用户不存在");
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        return user;
    }

    @Override
    public List<FamilyBinding> listBindings(Long operatorUserId, Integer operatorUserType,
                                            Long elderlyId, Long familyId, Integer status) {
        LambdaQueryWrapper<FamilyBinding> wrapper = new LambdaQueryWrapper<>();
        if (Objects.equals(operatorUserType, 3)) {
            if (elderlyId != null) {
                wrapper.eq(FamilyBinding::getElderlyId, elderlyId);
            }
            if (familyId != null) {
                wrapper.eq(FamilyBinding::getFamilyId, familyId);
            }
        } else if (Objects.equals(operatorUserType, 1)) {
            wrapper.eq(FamilyBinding::getElderlyId, operatorUserId);
        } else if (Objects.equals(operatorUserType, 2)) {
            wrapper.eq(FamilyBinding::getFamilyId, operatorUserId);
        } else {
            throw new ForbiddenException("当前用户无权查看绑定关系");
        }
        if (status != null) {
            wrapper.eq(FamilyBinding::getStatus, status);
        }
        wrapper.orderByDesc(FamilyBinding::getCreateTime);
        return familyBindingMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public FamilyBinding confirmBinding(Long bindingId, Long operatorUserId, Integer operatorUserType) {
        FamilyBinding binding = familyBindingMapper.selectById(bindingId);
        if (binding == null) {
            throw new BusinessException(404, "绑定申请不存在");
        }
        if (!Objects.equals(operatorUserType, 3) && !Objects.equals(binding.getElderlyId(), operatorUserId)) {
            throw new ForbiddenException("无权确认该绑定申请");
        }

        User elderlyUser = getRequiredUser(binding.getElderlyId(), "老人用户不存在");
        User familyUser = getRequiredUser(binding.getFamilyId(), "家属用户不存在");
        validateUserType(elderlyUser, 1, "老人用户不存在或类型不正确");
        validateUserType(familyUser, 2, "家属用户不存在或类型不正确");
        validateUserEnabled(elderlyUser, "老人用户未启用，暂不能确认绑定");
        validateUserEnabled(familyUser, "家属用户未启用，暂不能确认绑定");

        if (!Objects.equals(binding.getStatus(), 1)) {
            binding.setStatus(1);
            familyBindingMapper.updateById(binding);
        }
        return binding;
    }

    @Override
    public boolean hasConfirmedBinding(Long elderlyId, Long familyId) {
        LambdaQueryWrapper<FamilyBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyBinding::getElderlyId, elderlyId)
                .eq(FamilyBinding::getFamilyId, familyId)
                .eq(FamilyBinding::getStatus, 1)
                .last("LIMIT 1");
        return familyBindingMapper.selectOne(wrapper) != null;
    }

    @Override
    public List<Long> listConfirmedFamilyIds(Long elderlyId) {
        return familyBindingMapper.selectList(new LambdaQueryWrapper<FamilyBinding>()
                        .eq(FamilyBinding::getElderlyId, elderlyId)
                        .eq(FamilyBinding::getStatus, 1)
                        .orderByDesc(FamilyBinding::getCreateTime))
                .stream()
                .map(FamilyBinding::getFamilyId)
                .toList();
    }

    @Override
    public List<Long> listConfirmedElderlyIds(Long familyId) {
        return familyBindingMapper.selectList(new LambdaQueryWrapper<FamilyBinding>()
                        .eq(FamilyBinding::getFamilyId, familyId)
                        .eq(FamilyBinding::getStatus, 1)
                        .orderByDesc(FamilyBinding::getCreateTime))
                .stream()
                .map(FamilyBinding::getElderlyId)
                .toList();
    }

    @Override
    public ElderlyInfo getElderlyInfo(Long targetUserId, Long operatorUserId, Integer operatorUserType) {
        User targetUser = getRequiredUser(targetUserId, "老人用户不存在");
        validateUserType(targetUser, 1, "目标用户不是老人账号");
        if (!canViewElderlyInfo(targetUserId, operatorUserId, operatorUserType)) {
            throw new ForbiddenException("无权查看该老人资料");
        }
        return getElderlyInfoByUserId(targetUserId);
    }

    @Override
    @Transactional
    public ElderlyInfo saveElderlyInfo(Long targetUserId, ElderlyProfileRequest request) {
        User targetUser = getRequiredUser(targetUserId, "老人用户不存在");
        validateUserType(targetUser, 1, "目标用户不是老人账号");
        ElderlyInfo elderlyInfo = getElderlyInfoByUserId(targetUserId);
        LocalDateTime now = LocalDateTime.now();
        if (elderlyInfo == null) {
            elderlyInfo = new ElderlyInfo();
            elderlyInfo.setUserId(targetUserId);
            elderlyInfo.setCreateTime(now);
        }
        elderlyInfo.setAge(request.getAge());
        elderlyInfo.setGender(request.getGender());
        elderlyInfo.setHealthStatus(request.getHealthStatus());
        elderlyInfo.setMedicalHistory(request.getMedicalHistory());
        elderlyInfo.setLongitude(request.getLongitude());
        elderlyInfo.setLatitude(request.getLatitude());
        elderlyInfo.setUpdateTime(now);
        if (elderlyInfo.getId() == null) {
            elderlyInfoMapper.insert(elderlyInfo);
        } else {
            elderlyInfoMapper.updateById(elderlyInfo);
        }
        return elderlyInfo;
    }

    private void validateRegisterUserType(Integer userType) {
        if (!Objects.equals(userType, 1) && !Objects.equals(userType, 2) && !Objects.equals(userType, 4)) {
            throw new BusinessException(400, "公开注册仅支持老人、家属和服务人员用户");
        }
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequiredText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\s+", "");
    }

    private String normalizeOptionalPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.replaceAll("\\s+", "");
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (isEncodedPassword(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return Objects.equals(rawPassword, storedPassword);
    }

    private void upgradeLegacyPlaintextPassword(User user, String rawPassword) {
        if (user.getPassword() != null && !isEncodedPassword(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setUpdateTime(LocalDateTime.now());
            this.updateById(user);
        }
    }

    private User getRequiredUser(Long userId, String message) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, message);
        }
        return user;
    }

    private ElderlyInfo getElderlyInfoByUserId(Long userId) {
        LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyInfo::getUserId, userId).last("LIMIT 1");
        return elderlyInfoMapper.selectOne(wrapper);
    }

    private boolean canViewElderlyInfo(Long targetUserId, Long operatorUserId, Integer operatorUserType) {
        if (Objects.equals(operatorUserType, 3)) {
            return true;
        }
        if (Objects.equals(operatorUserId, targetUserId)) {
            return true;
        }
        return Objects.equals(operatorUserType, 2) && hasConfirmedBinding(targetUserId, operatorUserId);
    }

    private void validateUserType(User user, Integer expectedUserType, String message) {
        if (!Objects.equals(user.getUserType(), expectedUserType)) {
            throw new BusinessException(400, message);
        }
    }

    private void validateUserEnabled(User user, String message) {
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(400, message);
        }
    }

    private boolean isEncodedPassword(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
