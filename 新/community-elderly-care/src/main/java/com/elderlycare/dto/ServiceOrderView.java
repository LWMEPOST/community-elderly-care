package com.elderlycare.dto;

import com.elderlycare.entity.ServiceCategory;
import com.elderlycare.entity.ServiceItem;
import com.elderlycare.entity.ServiceOrder;
import com.elderlycare.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceOrderView {

    private Long id;
    private String orderNo;
    private Long elderlyId;
    private String elderlyName;
    private Long familyId;
    private String familyName;
    private Long serviceItemId;
    private String serviceItemName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal servicePrice;
    private Integer serviceDuration;
    private Long serviceUserId;
    private String serviceUserName;
    private LocalDateTime appointmentTime;
    private String serviceAddress;
    private Integer status;
    private String statusText;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ServiceOrderView from(ServiceOrder order,
                                        User elderlyUser,
                                        User familyUser,
                                        User serviceUser,
                                        ServiceItem serviceItem,
                                        ServiceCategory serviceCategory) {
        ServiceOrderView view = new ServiceOrderView();
        view.setId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setElderlyId(order.getElderlyId());
        view.setElderlyName(elderlyUser == null ? null : elderlyUser.getRealName());
        view.setFamilyId(order.getFamilyId());
        view.setFamilyName(familyUser == null ? null : familyUser.getRealName());
        view.setServiceItemId(order.getServiceItemId());
        view.setServiceItemName(serviceItem == null ? null : serviceItem.getName());
        view.setCategoryId(serviceItem == null ? null : serviceItem.getCategoryId());
        view.setCategoryName(serviceCategory == null ? null : serviceCategory.getName());
        view.setServicePrice(serviceItem == null ? null : serviceItem.getPrice());
        view.setServiceDuration(serviceItem == null ? null : serviceItem.getDuration());
        view.setServiceUserId(order.getServiceUserId());
        view.setServiceUserName(serviceUser == null ? null : serviceUser.getRealName());
        view.setAppointmentTime(order.getAppointmentTime());
        view.setServiceAddress(order.getServiceAddress());
        view.setStatus(order.getStatus());
        view.setStatusText(ServiceOrderWorkflow.statusText(order.getStatus()));
        view.setRemark(order.getRemark());
        view.setCreateTime(order.getCreateTime());
        view.setUpdateTime(order.getUpdateTime());
        return view;
    }
}
