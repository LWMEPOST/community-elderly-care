package com.elderlycare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elderlycare.dto.ServiceOrderCreateRequest;
import com.elderlycare.dto.ServiceOrderView;
import com.elderlycare.entity.ServiceCategory;
import com.elderlycare.entity.ServiceItem;

import java.util.List;

public interface ServiceService extends IService<ServiceCategory> {
    List<ServiceCategory> getCategories();
    List<ServiceItem> getServiceItems(Long categoryId);
    ServiceOrderView createOrder(Long operatorUserId, Integer operatorUserType, ServiceOrderCreateRequest request);
    List<ServiceOrderView> getOrders(Long operatorUserId, Integer operatorUserType, Integer status, String keyword);
    ServiceOrderView getOrderDetail(Long orderId, Long operatorUserId, Integer operatorUserType);
    ServiceOrderView assignOrder(Long orderId, Long serviceUserId, Long operatorUserId);
    ServiceOrderView acceptOrder(Long orderId, Long operatorUserId);
    ServiceOrderView startOrder(Long orderId, Long operatorUserId);
    ServiceOrderView completeOrder(Long orderId, Long operatorUserId);
    ServiceOrderView cancelOrder(Long orderId, Long operatorUserId, Integer operatorUserType);
}
