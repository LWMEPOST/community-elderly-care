package com.elderlycare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elderlycare.dto.ServiceOrderCreateRequest;
import com.elderlycare.dto.ServiceOrderView;
import com.elderlycare.dto.ServiceOrderWorkflow;
import com.elderlycare.entity.ServiceCategory;
import com.elderlycare.entity.ServiceItem;
import com.elderlycare.entity.ServiceOrder;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.mapper.ServiceCategoryMapper;
import com.elderlycare.mapper.ServiceItemMapper;
import com.elderlycare.mapper.ServiceOrderMapper;
import com.elderlycare.mapper.UserMapper;
import com.elderlycare.service.ServiceService;
import com.elderlycare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServiceServiceImpl extends ServiceImpl<ServiceCategoryMapper, ServiceCategory> implements ServiceService {

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private ServiceOrderMapper serviceOrderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Override
    public List<ServiceCategory> getCategories() {
        LambdaQueryWrapper<ServiceCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceCategory::getStatus, 1)
               .orderByAsc(ServiceCategory::getSortOrder)
               .orderByAsc(ServiceCategory::getId);
        return this.list(wrapper);
    }

    @Override
    public List<ServiceItem> getServiceItems(Long categoryId) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceItem::getCategoryId, categoryId)
               .eq(ServiceItem::getStatus, 1)
               .orderByAsc(ServiceItem::getId);
        return serviceItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public ServiceOrderView createOrder(Long operatorUserId, Integer operatorUserType, ServiceOrderCreateRequest request) {
        Long elderlyId = resolveElderlyIdForCreate(operatorUserId, operatorUserType, request);
        ServiceItem serviceItem = getActiveServiceItem(request.getServiceItemId());
        User elderlyUser = getRequiredUser(elderlyId, "老人用户不存在");
        validateUserType(elderlyUser, 1, "目标用户不是老人账号");
        validateEnabledUser(elderlyUser, "老人账号未启用");

        ServiceOrder order = new ServiceOrder();
        order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        order.setElderlyId(elderlyId);
        order.setFamilyId(operatorUserType == 2 ? operatorUserId : null);
        order.setServiceItemId(serviceItem.getId());
        order.setAppointmentTime(request.getAppointmentTime());
        order.setServiceAddress(request.getServiceAddress().trim());
        order.setRemark(request.getRemark());
        order.setStatus(ServiceOrderWorkflow.PENDING);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        serviceOrderMapper.insert(order);
        return buildOrderView(order);
    }

    @Override
    public List<ServiceOrderView> getOrders(Long operatorUserId, Integer operatorUserType, Integer status, String keyword) {
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<>();
        if (operatorUserType == 3) {
            if (keyword != null && !keyword.isBlank()) {
                wrapper.and(condition -> condition
                        .like(ServiceOrder::getOrderNo, keyword)
                        .or()
                        .like(ServiceOrder::getServiceAddress, keyword));
            }
        } else if (operatorUserType == 4) {
            wrapper.and(condition -> condition
                    .eq(ServiceOrder::getServiceUserId, operatorUserId)
                    .or(sub -> sub.isNull(ServiceOrder::getServiceUserId)
                            .eq(ServiceOrder::getStatus, ServiceOrderWorkflow.PENDING)));
        } else if (operatorUserType == 2) {
            wrapper.eq(ServiceOrder::getFamilyId, operatorUserId);
        } else {
            wrapper.eq(ServiceOrder::getElderlyId, operatorUserId);
        }
        if (status != null) {
            wrapper.eq(ServiceOrder::getStatus, status);
        }
        wrapper.orderByDesc(ServiceOrder::getCreateTime);
        return buildOrderViews(serviceOrderMapper.selectList(wrapper));
    }

    @Override
    public ServiceOrderView getOrderDetail(Long orderId, Long operatorUserId, Integer operatorUserType) {
        ServiceOrder order = getRequiredOrder(orderId);
        ensureOrderAccessible(order, operatorUserId, operatorUserType);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public ServiceOrderView assignOrder(Long orderId, Long serviceUserId, Long operatorUserId) {
        ServiceOrder order = getRequiredOrder(orderId);
        ensureAssignable(order);
        User serviceUser = getRequiredUser(serviceUserId, "服务人员不存在");
        validateUserType(serviceUser, 4, "目标用户不是服务人员");
        validateEnabledUser(serviceUser, "服务人员未启用");
        order.setServiceUserId(serviceUserId);
        order.setUpdateTime(LocalDateTime.now());
        serviceOrderMapper.updateById(order);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public ServiceOrderView acceptOrder(Long orderId, Long operatorUserId) {
        ServiceOrder order = getRequiredOrder(orderId);
        User serviceUser = getRequiredUser(operatorUserId, "服务人员不存在");
        validateUserType(serviceUser, 4, "当前用户不是服务人员");
        validateEnabledUser(serviceUser, "服务人员未启用");
        if (order.getStatus() != ServiceOrderWorkflow.PENDING) {
            throw new BusinessException(400, "只有待接单状态的订单可以接单");
        }
        if (order.getServiceUserId() != null && !order.getServiceUserId().equals(operatorUserId)) {
            throw new ForbiddenException("该订单已被指派给其他服务人员");
        }
        order.setServiceUserId(operatorUserId);
        order.setStatus(ServiceOrderWorkflow.ACCEPTED);
        order.setUpdateTime(LocalDateTime.now());
        serviceOrderMapper.updateById(order);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public ServiceOrderView startOrder(Long orderId, Long operatorUserId) {
        ServiceOrder order = getRequiredOrder(orderId);
        ensureServiceOwner(order, operatorUserId, ServiceOrderWorkflow.ACCEPTED, "只有已接单状态的订单可以开始服务");
        order.setStatus(ServiceOrderWorkflow.IN_SERVICE);
        order.setUpdateTime(LocalDateTime.now());
        serviceOrderMapper.updateById(order);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public ServiceOrderView completeOrder(Long orderId, Long operatorUserId) {
        ServiceOrder order = getRequiredOrder(orderId);
        ensureServiceOwner(order, operatorUserId, ServiceOrderWorkflow.IN_SERVICE, "只有服务中的订单可以完成");
        order.setStatus(ServiceOrderWorkflow.COMPLETED);
        order.setUpdateTime(LocalDateTime.now());
        serviceOrderMapper.updateById(order);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public ServiceOrderView cancelOrder(Long orderId, Long operatorUserId, Integer operatorUserType) {
        ServiceOrder order = getRequiredOrder(orderId);
        ensureCancelable(order, operatorUserId, operatorUserType);
        order.setStatus(ServiceOrderWorkflow.CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        serviceOrderMapper.updateById(order);
        return buildOrderView(order);
    }

    private Long resolveElderlyIdForCreate(Long operatorUserId, Integer operatorUserType, ServiceOrderCreateRequest request) {
        if (operatorUserType == 1) {
            Long elderlyId = request.getElderlyId() == null ? operatorUserId : request.getElderlyId();
            if (!elderlyId.equals(operatorUserId)) {
                throw new ForbiddenException("老人用户只能为自己创建订单");
            }
            return elderlyId;
        }
        if (operatorUserType == 2) {
            if (request.getElderlyId() == null) {
                throw new BusinessException(400, "家属下单时必须指定老人用户ID");
            }
            if (!userService.hasConfirmedBinding(request.getElderlyId(), operatorUserId)) {
                throw new ForbiddenException("只能为已确认绑定的老人创建订单");
            }
            return request.getElderlyId();
        }
        throw new ForbiddenException("当前用户无权创建服务订单");
    }

    private ServiceItem getActiveServiceItem(Long serviceItemId) {
        ServiceItem serviceItem = serviceItemMapper.selectById(serviceItemId);
        if (serviceItem == null || serviceItem.getStatus() == null || serviceItem.getStatus() != 1) {
            throw new BusinessException(404, "服务项目不存在或未上架");
        }
        return serviceItem;
    }

    private ServiceOrder getRequiredOrder(Long orderId) {
        ServiceOrder order = serviceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    private User getRequiredUser(Long userId, String message) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, message);
        }
        return user;
    }

    private void validateUserType(User user, Integer expectedUserType, String message) {
        if (!expectedUserType.equals(user.getUserType())) {
            throw new BusinessException(400, message);
        }
    }

    private void validateEnabledUser(User user, String message) {
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(400, message);
        }
    }

    private void ensureOrderAccessible(ServiceOrder order, Long operatorUserId, Integer operatorUserType) {
        boolean accessible = switch (operatorUserType) {
            case 3 -> true;
            case 4 -> operatorUserId.equals(order.getServiceUserId())
                    || (order.getServiceUserId() == null && order.getStatus() == ServiceOrderWorkflow.PENDING);
            case 2 -> operatorUserId.equals(order.getFamilyId());
            case 1 -> operatorUserId.equals(order.getElderlyId());
            default -> false;
        };
        if (!accessible) {
            throw new ForbiddenException("无权查看该订单");
        }
    }

    private void ensureAssignable(ServiceOrder order) {
        if (order.getStatus() != ServiceOrderWorkflow.PENDING) {
            throw new BusinessException(400, "只有待接单状态的订单可以派单");
        }
    }

    private void ensureServiceOwner(ServiceOrder order, Long operatorUserId, int expectedStatus, String statusErrorMessage) {
        if (order.getStatus() != expectedStatus) {
            throw new BusinessException(400, statusErrorMessage);
        }
        if (order.getServiceUserId() == null || !order.getServiceUserId().equals(operatorUserId)) {
            throw new ForbiddenException("只有当前接单的服务人员才能操作该订单");
        }
    }

    private void ensureCancelable(ServiceOrder order, Long operatorUserId, Integer operatorUserType) {
        if (order.getStatus() != ServiceOrderWorkflow.PENDING
                && order.getStatus() != ServiceOrderWorkflow.ACCEPTED) {
            throw new BusinessException(400, "只有待接单或已接单状态的订单可以取消");
        }
        boolean allowed = switch (operatorUserType) {
            case 3 -> true;
            case 2 -> operatorUserId.equals(order.getFamilyId());
            case 1 -> operatorUserId.equals(order.getElderlyId());
            default -> false;
        };
        if (!allowed) {
            throw new ForbiddenException("无权取消该订单");
        }
    }

    private List<ServiceOrderView> buildOrderViews(List<ServiceOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> itemIds = new HashSet<>();
        orders.forEach(order -> {
            userIds.add(order.getElderlyId());
            if (order.getFamilyId() != null) {
                userIds.add(order.getFamilyId());
            }
            if (order.getServiceUserId() != null) {
                userIds.add(order.getServiceUserId());
            }
            itemIds.add(order.getServiceItemId());
        });

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, ServiceItem> itemMap = serviceItemMapper.selectBatchIds(itemIds)
                .stream()
                .collect(Collectors.toMap(ServiceItem::getId, Function.identity()));
        Set<Long> categoryIds = itemMap.values()
                .stream()
                .map(ServiceItem::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, ServiceCategory> categoryMap = baseMapper.selectBatchIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(ServiceCategory::getId, Function.identity()));

        return orders.stream()
                .map(order -> {
                    ServiceItem item = itemMap.get(order.getServiceItemId());
                    ServiceCategory category = item == null ? null : categoryMap.get(item.getCategoryId());
                    return ServiceOrderView.from(
                            order,
                            userMap.get(order.getElderlyId()),
                            userMap.get(order.getFamilyId()),
                            userMap.get(order.getServiceUserId()),
                            item,
                            category);
                })
                .collect(Collectors.toList());
    }

    private ServiceOrderView buildOrderView(ServiceOrder order) {
        return buildOrderViews(List.of(order)).get(0);
    }
}
