package com.elderlycare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elderlycare.dto.EmergencyHelpCreateRequest;
import com.elderlycare.dto.EmergencyHelpView;
import com.elderlycare.dto.EmergencyHelpWorkflow;
import com.elderlycare.entity.EmergencyHelp;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.mapper.EmergencyHelpMapper;
import com.elderlycare.service.EmergencyService;
import com.elderlycare.service.MessageService;
import com.elderlycare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmergencyServiceImpl extends ServiceImpl<EmergencyHelpMapper, EmergencyHelp> implements EmergencyService {

    @Autowired
    private EmergencyHelpMapper emergencyHelpMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional
    public EmergencyHelpView createEmergency(Long operatorUserId, Integer operatorUserType, EmergencyHelpCreateRequest request) {
        Long elderlyId = resolveCreateTargetElderlyId(operatorUserId, operatorUserType, request.getElderlyId());
        User elderlyUser = getRequiredElderlyUser(elderlyId);
        validateCoordinates(request.getLongitude(), request.getLatitude());

        EmergencyHelp emergency = new EmergencyHelp();
        emergency.setElderlyId(elderlyId);
        emergency.setLongitude(request.getLongitude());
        emergency.setLatitude(request.getLatitude());
        emergency.setLocationAddress(normalizeText(request.getLocationAddress()));
        emergency.setHelpType(request.getHelpType());
        emergency.setDescription(normalizeText(request.getDescription()));
        emergency.setStatus(EmergencyHelpWorkflow.STATUS_PENDING);
        emergency.setCreateTime(LocalDateTime.now());
        emergencyHelpMapper.insert(emergency);

        notifyEmergencyCreated(emergency, operatorUserId, elderlyUser);
        return buildView(emergency, elderlyUser, null, operatorUserId, operatorUserType);
    }

    @Override
    public List<EmergencyHelpView> getEmergencyList(Long operatorUserId, Integer operatorUserType,
                                                    Long elderlyId, Integer status, Integer limit) {
        LambdaQueryWrapper<EmergencyHelp> wrapper = new LambdaQueryWrapper<>();
        switch (operatorUserType) {
            case 3, 4 -> {
                if (elderlyId != null) {
                    getRequiredElderlyUser(elderlyId);
                    wrapper.eq(EmergencyHelp::getElderlyId, elderlyId);
                }
            }
            case 1 -> {
                if (elderlyId != null && !Objects.equals(elderlyId, operatorUserId)) {
                    throw new ForbiddenException("无权查看该老人求助记录");
                }
                wrapper.eq(EmergencyHelp::getElderlyId, operatorUserId);
            }
            case 2 -> {
                if (elderlyId != null) {
                    if (!userService.hasConfirmedBinding(elderlyId, operatorUserId)) {
                        throw new ForbiddenException("无权查看该老人求助记录");
                    }
                    wrapper.eq(EmergencyHelp::getElderlyId, elderlyId);
                } else {
                    List<Long> elderlyIds = userService.listConfirmedElderlyIds(operatorUserId);
                    if (elderlyIds.isEmpty()) {
                        return List.of();
                    }
                    wrapper.in(EmergencyHelp::getElderlyId, elderlyIds);
                }
            }
            default -> throw new ForbiddenException("当前用户无权查看求助记录");
        }
        wrapper.eq(status != null, EmergencyHelp::getStatus, status);
        wrapper.orderByDesc(EmergencyHelp::getCreateTime);
        if (limit != null) {
            wrapper.last("LIMIT " + normalizeLimit(limit));
        }
        return buildViews(emergencyHelpMapper.selectList(wrapper), operatorUserId, operatorUserType);
    }

    @Override
    public EmergencyHelpView getEmergencyDetail(Long emergencyId, Long operatorUserId, Integer operatorUserType) {
        EmergencyHelp emergency = getRequiredEmergency(emergencyId);
        ensureEmergencyAccess(emergency, operatorUserId, operatorUserType);
        User elderlyUser = userService.getUserInfo(emergency.getElderlyId());
        User responseUser = emergency.getResponseUserId() == null ? null : userService.getUserInfo(emergency.getResponseUserId());
        return buildView(emergency, elderlyUser, responseUser, operatorUserId, operatorUserType);
    }

    @Override
    @Transactional
    public EmergencyHelpView respondEmergency(Long emergencyId, Long operatorUserId,
                                              Integer operatorUserType, Long responseUserId) {
        EmergencyHelp emergency = getRequiredEmergency(emergencyId);
        if (!Objects.equals(emergency.getStatus(), EmergencyHelpWorkflow.STATUS_PENDING)) {
            throw new BusinessException(400, "当前求助不处于待响应状态");
        }
        Long targetResponseUserId = resolveResponseUserId(operatorUserId, operatorUserType, responseUserId);
        User responseUser = getRequiredUser(targetResponseUserId, "响应人员不存在");
        validateResponder(responseUser);

        emergency.setStatus(EmergencyHelpWorkflow.STATUS_RESPONDED);
        emergency.setResponseUserId(targetResponseUserId);
        emergencyHelpMapper.updateById(emergency);

        User elderlyUser = getRequiredElderlyUser(emergency.getElderlyId());
        notifyEmergencyProgress(emergency, targetResponseUserId,
                "已由" + displayName(responseUser) + "响应", elderlyUser);
        return buildView(emergency, elderlyUser, responseUser, operatorUserId, operatorUserType);
    }

    @Override
    @Transactional
    public EmergencyHelpView resolveEmergency(Long emergencyId, Long operatorUserId, Integer operatorUserType) {
        EmergencyHelp emergency = getRequiredEmergency(emergencyId);
        if (!Objects.equals(emergency.getStatus(), EmergencyHelpWorkflow.STATUS_RESPONDED)) {
            throw new BusinessException(400, "当前求助不处于已响应状态");
        }
        if (Objects.equals(operatorUserType, 4) && !Objects.equals(emergency.getResponseUserId(), operatorUserId)) {
            throw new ForbiddenException("仅当前响应人可解决该求助");
        }

        emergency.setStatus(EmergencyHelpWorkflow.STATUS_RESOLVED);
        emergency.setResolveTime(LocalDateTime.now());
        emergencyHelpMapper.updateById(emergency);

        User elderlyUser = getRequiredElderlyUser(emergency.getElderlyId());
        User responseUser = emergency.getResponseUserId() == null ? null : userService.getUserInfo(emergency.getResponseUserId());
        notifyEmergencyProgress(emergency, operatorUserId, "已处理完成", elderlyUser);
        return buildView(emergency, elderlyUser, responseUser, operatorUserId, operatorUserType);
    }

    private Long resolveCreateTargetElderlyId(Long operatorUserId, Integer operatorUserType, Long elderlyId) {
        return switch (operatorUserType) {
            case 1 -> {
                if (elderlyId != null && !Objects.equals(elderlyId, operatorUserId)) {
                    throw new ForbiddenException("老人仅能为自己发起求助");
                }
                yield operatorUserId;
            }
            case 2 -> {
                if (elderlyId == null) {
                    throw new BusinessException(400, "家属发起求助时必须指定老人用户ID");
                }
                if (!userService.hasConfirmedBinding(elderlyId, operatorUserId)) {
                    throw new ForbiddenException("无权为该老人发起求助");
                }
                yield elderlyId;
            }
            case 3 -> {
                if (elderlyId == null) {
                    throw new BusinessException(400, "请指定老人用户ID");
                }
                yield elderlyId;
            }
            default -> throw new ForbiddenException("当前用户无权发起求助");
        };
    }

    private void ensureEmergencyAccess(EmergencyHelp emergency, Long operatorUserId, Integer operatorUserType) {
        boolean allowed = switch (operatorUserType) {
            case 3, 4 -> true;
            case 1 -> Objects.equals(emergency.getElderlyId(), operatorUserId);
            case 2 -> userService.hasConfirmedBinding(emergency.getElderlyId(), operatorUserId);
            default -> false;
        };
        if (!allowed) {
            throw new ForbiddenException("无权查看该求助记录");
        }
    }

    private Long resolveResponseUserId(Long operatorUserId, Integer operatorUserType, Long responseUserId) {
        if (Objects.equals(operatorUserType, 4)) {
            if (responseUserId != null && !Objects.equals(responseUserId, operatorUserId)) {
                throw new ForbiddenException("服务人员仅能由本人响应求助");
            }
            return operatorUserId;
        }
        return responseUserId == null ? operatorUserId : responseUserId;
    }

    private void validateResponder(User responseUser) {
        if (!Objects.equals(responseUser.getStatus(), 1)) {
            throw new BusinessException(400, "响应人员账号未启用");
        }
        if (!Objects.equals(responseUser.getUserType(), 3) && !Objects.equals(responseUser.getUserType(), 4)) {
            throw new BusinessException(400, "响应人员必须为管理员或服务人员");
        }
    }

    private EmergencyHelp getRequiredEmergency(Long emergencyId) {
        EmergencyHelp emergency = emergencyHelpMapper.selectById(emergencyId);
        if (emergency == null) {
            throw new BusinessException(404, "求助记录不存在");
        }
        return emergency;
    }

    private User getRequiredElderlyUser(Long elderlyId) {
        User elderlyUser = getRequiredUser(elderlyId, "老人用户不存在");
        if (!Objects.equals(elderlyUser.getUserType(), 1)) {
            throw new BusinessException(400, "目标用户不是老人账号");
        }
        if (!Objects.equals(elderlyUser.getStatus(), 1)) {
            throw new BusinessException(400, "老人账号未启用");
        }
        return elderlyUser;
    }

    private User getRequiredUser(Long userId, String message) {
        User user = userService.getUserInfo(userId);
        if (user == null) {
            throw new BusinessException(404, message);
        }
        return user;
    }

    private List<EmergencyHelpView> buildViews(List<EmergencyHelp> emergencies, Long operatorUserId, Integer operatorUserType) {
        if (emergencies.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = emergencies.stream()
                .flatMap(emergency -> java.util.stream.Stream.of(emergency.getElderlyId(), emergency.getResponseUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return emergencies.stream()
                .map(emergency -> buildView(
                        emergency,
                        userMap.get(emergency.getElderlyId()),
                        userMap.get(emergency.getResponseUserId()),
                        operatorUserId,
                        operatorUserType))
                .toList();
    }

    private EmergencyHelpView buildView(EmergencyHelp emergency, User elderlyUser, User responseUser,
                                        Long operatorUserId, Integer operatorUserType) {
        EmergencyHelpView view = EmergencyHelpView.from(emergency, elderlyUser, responseUser);
        view.setCanRespond((Objects.equals(operatorUserType, 3) || Objects.equals(operatorUserType, 4))
                && Objects.equals(emergency.getStatus(), EmergencyHelpWorkflow.STATUS_PENDING));
        view.setCanResolve(Objects.equals(emergency.getStatus(), EmergencyHelpWorkflow.STATUS_RESPONDED)
                && (Objects.equals(operatorUserType, 3)
                || (Objects.equals(operatorUserType, 4) && Objects.equals(emergency.getResponseUserId(), operatorUserId))));
        return view;
    }

    private void notifyEmergencyCreated(EmergencyHelp emergency, Long operatorUserId, User elderlyUser) {
        Set<Long> receiverIds = new LinkedHashSet<>();
        receiverIds.addAll(listActiveAdminIds());
        receiverIds.addAll(userService.listConfirmedFamilyIds(emergency.getElderlyId()));
        receiverIds.remove(operatorUserId);
        if (receiverIds.isEmpty()) {
            return;
        }
        String content = "紧急求助提醒：" + displayName(elderlyUser)
                + "发起了" + EmergencyHelpWorkflow.helpTypeText(emergency.getHelpType())
                + "，位置：" + buildLocationSummary(emergency) + "。";
        messageService.sendSystemNotifications(operatorUserId, receiverIds, content);
    }

    private void notifyEmergencyProgress(EmergencyHelp emergency, Long operatorUserId, String progressText, User elderlyUser) {
        Set<Long> receiverIds = new LinkedHashSet<>();
        receiverIds.add(emergency.getElderlyId());
        receiverIds.addAll(userService.listConfirmedFamilyIds(emergency.getElderlyId()));
        receiverIds.remove(operatorUserId);
        if (receiverIds.isEmpty()) {
            return;
        }
        String content = "应急进展提醒：" + displayName(elderlyUser)
                + "的" + EmergencyHelpWorkflow.helpTypeText(emergency.getHelpType())
                + progressText + "。";
        messageService.sendSystemNotifications(operatorUserId, receiverIds, content);
    }

    private List<Long> listActiveAdminIds() {
        return userService.list(new LambdaQueryWrapper<User>()
                        .eq(User::getUserType, 3)
                        .eq(User::getStatus, 1))
                .stream()
                .map(User::getId)
                .toList();
    }

    private String buildLocationSummary(EmergencyHelp emergency) {
        if (StringUtils.hasText(emergency.getLocationAddress())) {
            return emergency.getLocationAddress().trim();
        }
        if (emergency.getLongitude() != null && emergency.getLatitude() != null) {
            return emergency.getLongitude() + "," + emergency.getLatitude();
        }
        return "位置未提供";
    }

    private String displayName(User user) {
        if (user == null) {
            return "用户";
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "用户#" + user.getId();
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateCoordinates(java.math.BigDecimal longitude, java.math.BigDecimal latitude) {
        if ((longitude == null) != (latitude == null)) {
            throw new BusinessException(400, "经纬度需同时提供");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
