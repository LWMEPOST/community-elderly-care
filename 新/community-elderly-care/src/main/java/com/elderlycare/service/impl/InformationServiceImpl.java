package com.elderlycare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elderlycare.dto.InformationRulebook;
import com.elderlycare.dto.InformationSaveRequest;
import com.elderlycare.dto.InformationView;
import com.elderlycare.entity.Information;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.mapper.InformationMapper;
import com.elderlycare.service.InformationService;
import com.elderlycare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InformationServiceImpl extends ServiceImpl<InformationMapper, Information> implements InformationService {

    @Autowired
    private InformationMapper informationMapper;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public InformationView createDraft(Long operatorUserId, InformationSaveRequest request) {
        requireActiveAdmin(operatorUserId);
        Information information = buildInformationEntity(request, operatorUserId);
        information.setStatus(InformationRulebook.STATUS_DRAFT);
        information.setViewCount(0);
        information.setCreateTime(LocalDateTime.now());
        informationMapper.insert(information);
        return buildView(information, true);
    }

    @Override
    @Transactional
    public InformationView createAndPublish(Long operatorUserId, InformationSaveRequest request) {
        requireActiveAdmin(operatorUserId);
        Information information = buildInformationEntity(request, operatorUserId);
        information.setStatus(InformationRulebook.STATUS_PUBLISHED);
        information.setViewCount(0);
        information.setCreateTime(LocalDateTime.now());
        information.setPublishTime(LocalDateTime.now());
        informationMapper.insert(information);
        return buildView(information, true);
    }

    @Override
    @Transactional
    public InformationView updateInformation(Long informationId, Long operatorUserId, InformationSaveRequest request) {
        Information information = getRequiredInformation(informationId);
        ensureAdminPublisher(operatorUserId, information);
        applyInformationContent(information, request);
        informationMapper.updateById(information);
        return buildView(information, true);
    }

    @Override
    @Transactional
    public InformationView publishInformation(Long informationId, Long operatorUserId) {
        Information information = getRequiredInformation(informationId);
        ensureAdminPublisher(operatorUserId, information);
        if (Objects.equals(information.getStatus(), InformationRulebook.STATUS_PUBLISHED)) {
            throw new BusinessException(400, "资讯已处于发布状态");
        }
        information.setStatus(InformationRulebook.STATUS_PUBLISHED);
        information.setPublishTime(LocalDateTime.now());
        informationMapper.updateById(information);
        return buildView(information, true);
    }

    @Override
    @Transactional
    public InformationView withdrawInformation(Long informationId, Long operatorUserId) {
        Information information = getRequiredInformation(informationId);
        ensureAdminPublisher(operatorUserId, information);
        if (!Objects.equals(information.getStatus(), InformationRulebook.STATUS_PUBLISHED)) {
            throw new BusinessException(400, "仅已发布资讯可撤回");
        }
        information.setStatus(InformationRulebook.STATUS_DRAFT);
        informationMapper.updateById(information);
        return buildView(information, true);
    }

    @Override
    public List<InformationView> getPublishedInformationList(Integer infoType, String keyword, Integer limit) {
        LambdaQueryWrapper<Information> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Information::getStatus, InformationRulebook.STATUS_PUBLISHED)
                .eq(infoType != null, Information::getInfoType, infoType)
                .and(hasKeyword(keyword), condition -> condition
                        .like(Information::getTitle, keyword.trim())
                        .or()
                        .like(Information::getContent, keyword.trim()))
                .orderByDesc(Information::getPublishTime)
                .orderByDesc(Information::getCreateTime);
        if (limit != null) {
            wrapper.last("LIMIT " + normalizeLimit(limit));
        }
        return buildViews(informationMapper.selectList(wrapper), false);
    }

    @Override
    public List<InformationView> getManageInformationList(Integer infoType, Integer status, String keyword,
                                                          Long publisherId, Integer limit) {
        LambdaQueryWrapper<Information> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(infoType != null, Information::getInfoType, infoType)
                .eq(status != null, Information::getStatus, status)
                .eq(publisherId != null, Information::getPublisherId, publisherId)
                .and(hasKeyword(keyword), condition -> condition
                        .like(Information::getTitle, keyword.trim())
                        .or()
                        .like(Information::getContent, keyword.trim()))
                .orderByDesc(Information::getPublishTime)
                .orderByDesc(Information::getCreateTime);
        if (limit != null) {
            wrapper.last("LIMIT " + normalizeLimit(limit));
        }
        return buildViews(informationMapper.selectList(wrapper), true);
    }

    @Override
    public InformationView getInformationDetail(Long id, Long operatorUserId, Integer operatorUserType) {
        Information information = getRequiredInformation(id);
        boolean isAdmin = Objects.equals(operatorUserType, 3);
        if (!isAdmin && !Objects.equals(information.getStatus(), InformationRulebook.STATUS_PUBLISHED)) {
            throw new ForbiddenException("当前资讯未发布，暂不可查看");
        }
        if (!isAdmin && Objects.equals(information.getStatus(), InformationRulebook.STATUS_PUBLISHED)) {
            incrementViewCount(id);
            information = getRequiredInformation(id);
        }
        return buildView(information, isAdmin);
    }

    @Override
    public boolean incrementViewCount(Long id) {
        Information information = informationMapper.selectById(id);
        if (information != null) {
            Integer viewCount = information.getViewCount() == null ? 0 : information.getViewCount();
            information.setViewCount(viewCount + 1);
            return informationMapper.updateById(information) > 0;
        }
        return false;
    }

    private Information buildInformationEntity(InformationSaveRequest request, Long publisherId) {
        Information information = new Information();
        information.setPublisherId(publisherId);
        information.setViewCount(0);
        applyInformationContent(information, request);
        return information;
    }

    private void applyInformationContent(Information information, InformationSaveRequest request) {
        information.setTitle(request.getTitle().trim());
        information.setContent(request.getContent().trim());
        information.setInfoType(request.getInfoType());
        information.setCoverImage(normalizeText(request.getCoverImage()));
    }

    private Information getRequiredInformation(Long id) {
        Information information = informationMapper.selectById(id);
        if (information == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        return information;
    }

    private void requireActiveAdmin(Long operatorUserId) {
        User user = getRequiredUser(operatorUserId);
        if (!Objects.equals(user.getUserType(), 3) || !Objects.equals(user.getStatus(), 1)) {
            throw new ForbiddenException("当前用户无权管理资讯");
        }
    }

    private void ensureAdminPublisher(Long operatorUserId, Information information) {
        requireActiveAdmin(operatorUserId);
        if (information.getPublisherId() == null) {
            information.setPublisherId(operatorUserId);
        }
    }

    private List<InformationView> buildViews(List<Information> informations, boolean adminMode) {
        if (informations.isEmpty()) {
            return List.of();
        }
        Set<Long> publisherIds = informations.stream()
                .map(Information::getPublisherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> publisherMap = userService.listByIds(publisherIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return informations.stream()
                .map(information -> buildView(information, publisherMap.get(information.getPublisherId()), adminMode))
                .toList();
    }

    private InformationView buildView(Information information, boolean adminMode) {
        return buildView(information, getRequiredUser(information.getPublisherId()), adminMode);
    }

    private InformationView buildView(Information information, User publisher, boolean adminMode) {
        InformationView view = InformationView.from(information, publisher);
        view.setCanEdit(adminMode);
        view.setCanPublish(adminMode && Objects.equals(information.getStatus(), InformationRulebook.STATUS_DRAFT));
        view.setCanWithdraw(adminMode && Objects.equals(information.getStatus(), InformationRulebook.STATUS_PUBLISHED));
        return view;
    }

    private User getRequiredUser(Long userId) {
        User user = userService.getUserInfo(userId);
        if (user == null) {
            throw new BusinessException(404, "发布人不存在");
        }
        return user;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.trim().isEmpty();
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
