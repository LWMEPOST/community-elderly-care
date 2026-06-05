package com.elderlycare.dto;

import com.elderlycare.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserView {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private Integer userType;
    private String avatar;
    private String address;
    private String emergencyContact;
    private String emergencyPhone;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static UserView from(User user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setRealName(user.getRealName());
        view.setPhone(user.getPhone());
        view.setUserType(user.getUserType());
        view.setAvatar(user.getAvatar());
        view.setAddress(user.getAddress());
        view.setEmergencyContact(user.getEmergencyContact());
        view.setEmergencyPhone(user.getEmergencyPhone());
        view.setStatus(user.getStatus());
        view.setCreateTime(user.getCreateTime());
        view.setUpdateTime(user.getUpdateTime());
        return view;
    }
}
