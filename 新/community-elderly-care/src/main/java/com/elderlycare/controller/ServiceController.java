package com.elderlycare.controller;

import com.elderlycare.auth.AuthenticatedUser;
import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.auth.UserContextHolder;
import com.elderlycare.common.Result;
import com.elderlycare.dto.ServiceOrderAssignRequest;
import com.elderlycare.dto.ServiceOrderCreateRequest;
import com.elderlycare.dto.ServiceOrderView;
import com.elderlycare.entity.ServiceCategory;
import com.elderlycare.entity.ServiceItem;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/service")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @GetMapping("/categories")
    public Result<List<ServiceCategory>> getCategories() {
        List<ServiceCategory> categories = serviceService.getCategories();
        return Result.success(categories);
    }

    @GetMapping("/items/{categoryId}")
    public Result<List<ServiceItem>> getServiceItems(@PathVariable Long categoryId) {
        List<ServiceItem> items = serviceService.getServiceItems(categoryId);
        return Result.success(items);
    }

    @RequireUserTypes({1, 2})
    @PostMapping("/order")
    public Result<ServiceOrderView> createOrder(@Valid @RequestBody ServiceOrderCreateRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView createdOrder = serviceService.createOrder(
                currentUser.getUserId(), currentUser.getUserType(), request);
        return Result.success("订单创建成功", createdOrder);
    }

    @GetMapping("/orders")
    public Result<List<ServiceOrderView>> getOrders(@RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String keyword) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        List<ServiceOrderView> orders = serviceService.getOrders(
                currentUser.getUserId(), currentUser.getUserType(), status, keyword);
        return Result.success(orders);
    }

    @GetMapping("/order/{id}")
    public Result<ServiceOrderView> getOrderDetail(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView order = serviceService.getOrderDetail(
                id, currentUser.getUserId(), currentUser.getUserType());
        return Result.success(order);
    }

    @RequireUserTypes({3})
    @PutMapping("/order/{id}/assign")
    public Result<ServiceOrderView> assignOrder(@PathVariable Long id,
                                                @Valid @RequestBody ServiceOrderAssignRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView order = serviceService.assignOrder(id, request.getServiceUserId(), currentUser.getUserId());
        return Result.success("派单成功", order);
    }

    @RequireUserTypes({4})
    @PutMapping("/order/{id}/accept")
    public Result<ServiceOrderView> acceptOrder(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView order = serviceService.acceptOrder(id, currentUser.getUserId());
        return Result.success("接单成功", order);
    }

    @RequireUserTypes({4})
    @PutMapping("/order/{id}/start")
    public Result<ServiceOrderView> startOrder(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView order = serviceService.startOrder(id, currentUser.getUserId());
        return Result.success("服务已开始", order);
    }

    @RequireUserTypes({4})
    @PutMapping("/order/{id}/complete")
    public Result<ServiceOrderView> completeOrder(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView order = serviceService.completeOrder(id, currentUser.getUserId());
        return Result.success("订单已完成", order);
    }

    @RequireUserTypes({1, 2, 3})
    @PutMapping("/order/{id}/cancel")
    public Result<ServiceOrderView> cancelOrder(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView order = serviceService.cancelOrder(
                id, currentUser.getUserId(), currentUser.getUserType());
        return Result.success("订单已取消", order);
    }

    @RequireUserTypes({3, 4})
    @PutMapping("/order/{id}/status")
    public Result<ServiceOrderView> updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        ServiceOrderView order = switch (status) {
            case 2 -> serviceService.acceptOrder(id, currentUser.getUserId());
            case 3 -> serviceService.startOrder(id, currentUser.getUserId());
            case 4 -> serviceService.completeOrder(id, currentUser.getUserId());
            case 5 -> serviceService.cancelOrder(id, currentUser.getUserId(), currentUser.getUserType());
            default -> throw new BusinessException(400, "不支持直接更新为该状态，请使用明确的业务动作接口");
        };
        return Result.success("订单状态更新成功", order);
    }
}
